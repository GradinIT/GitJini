package net.jini.grid;

import net.jini.core.export.ExportedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ServiceUnitLoader {

    public static ServiceUnit load(File unitPath) throws Exception {
        if (!unitPath.exists()) {
            throw new IllegalArgumentException("Service Unit path does not exist: " + unitPath.getAbsolutePath());
        }

        String name = unitPath.getName();
        if (name.endsWith(".jar") || name.endsWith(".war")) {
            name = name.substring(0, name.lastIndexOf("."));
            return loadFromJar(unitPath, name);
        }

        File metaInf = new File(unitPath, "META-INF");
        File springDir = new File(metaInf, "spring");

        // Load SLA
        SLA sla = loadSla(unitPath, springDir);

        // Load pu.xml (mandatory)
        File puXml = new File(springDir, "pu.xml");
        if (!puXml.exists()) {
            throw new IllegalArgumentException("pu.xml is mandatory and must be at META-INF/spring/pu.xml");
        }

        List<Object> services = new ArrayList<>();
        String[] spaceUrl = new String[1];
        boolean embeddedSpace = parsePuXml(puXml, services, spaceUrl);

        ServiceUnit unit = new ServiceUnit(name, services, sla, embeddedSpace);
        unit.setSpaceUrl(spaceUrl[0]);
        return unit;
    }

    private static ServiceUnit loadFromJar(File jarFile, String name) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {
            SLA sla = loadSlaFromJar(jar);
            
            JarEntry puXmlEntry = jar.getJarEntry("META-INF/spring/pu.xml");
            if (puXmlEntry == null) {
                throw new IllegalArgumentException("pu.xml is mandatory and must be at META-INF/spring/pu.xml in the JAR");
            }

            List<Object> services = new ArrayList<>();
            String[] spaceUrl = new String[1];
            boolean embeddedSpace;
            try (InputStream is = jar.getInputStream(puXmlEntry)) {
                embeddedSpace = parsePuXmlFromStream(is, services, spaceUrl);
            }

            ServiceUnit unit = new ServiceUnit(name, services, sla, embeddedSpace);
            unit.setSpaceUrl(spaceUrl[0]);
            return unit;
        }
    }

    private static SLA loadSlaFromJar(JarFile jar) throws Exception {
        JarEntry slaEntry = jar.getJarEntry("META-INF/spring/sla.xml");
        if (slaEntry == null) {
            slaEntry = jar.getJarEntry("sla.xml");
        }

        if (slaEntry != null) {
            try (InputStream is = jar.getInputStream(slaEntry)) {
                return parseSlaFromStream(is);
            }
        }
        return new SLA();
    }

    private static SLA parseSlaFromStream(InputStream is) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        SLA sla = new SLA();
        NodeList nList = doc.getElementsByTagNameNS("http://www.openspaces.org/schema/sla", "sla");
        if (nList.getLength() > 0) {
            Element element = (Element) nList.item(0);
            if (element.hasAttribute("cluster-schema")) {
                sla.setClusterSchema(element.getAttribute("cluster-schema"));
            }
            if (element.hasAttribute("number-of-instances")) {
                sla.setNumberOfInstances(Integer.parseInt(element.getAttribute("number-of-instances")));
            }
            if (element.hasAttribute("number-of-backups")) {
                sla.setNumberOfBackups(Integer.parseInt(element.getAttribute("number-of-backups")));
            }
            if (element.hasAttribute("max-instances-per-vm")) {
                sla.setMaxInstancesPerVM(Integer.parseInt(element.getAttribute("max-instances-per-vm")));
            }
        }
        return sla;
    }

    private static boolean parsePuXmlFromStream(InputStream is, List<Object> services, String[] spaceUrl) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        boolean embeddedSpace = false;

        // Simulate finding <os-core:space id="space" url="/./space" />
        NodeList spaceList = doc.getElementsByTagNameNS("http://www.openspaces.org/schema/core", "space");
        if (spaceList.getLength() > 0) {
            embeddedSpace = true;
            Element spaceElem = (Element) spaceList.item(0);
            if (spaceElem.hasAttribute("url")) {
                spaceUrl[0] = spaceElem.getAttribute("url");
            }
        }

        // Handle EmbeddedSpaceFactoryBean
        NodeList beanList = doc.getElementsByTagName("bean");
        for (int i = 0; i < beanList.getLength(); i++) {
            Element bean = (Element) beanList.item(i);
            String className = bean.getAttribute("class");
            if ("net.jini.space.EmbeddedSpaceFactoryBean".equals(className)) {
                embeddedSpace = true;
                // Try to find name property
                NodeList props = bean.getElementsByTagName("property");
                for (int j = 0; j < props.getLength(); j++) {
                    Element prop = (Element) props.item(j);
                    if ("name".equals(prop.getAttribute("name"))) {
                        spaceUrl[0] = "/./" + prop.getAttribute("value");
                    }
                }
            }
        }

        // Simulate finding <bean class="..." />
        NodeList beans = doc.getElementsByTagName("bean");
        for (int i = 0; i < beans.getLength(); i++) {
            Element bean = (Element) beans.item(i);
            String className = bean.getAttribute("class");
            if (!className.isEmpty()) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(ExportedService.class)) {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        services.add(instance);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Could not load bean class: " + className);
                }
            }
        }

        return embeddedSpace;
    }

    private static SLA loadSla(File root, File springDir) throws Exception {
        File slaXml = new File(springDir, "sla.xml");
        if (!slaXml.exists()) {
            slaXml = new File(root, "sla.xml");
        }

        if (slaXml.exists()) {
            return DeploymentUtility.loadSLA(slaXml.getAbsolutePath());
        }
        return new SLA();
    }

    private static boolean parsePuXml(File puXml, List<Object> services, String[] spaceUrl) throws Exception {
        try (InputStream is = new java.io.FileInputStream(puXml)) {
            return parsePuXmlFromStream(is, services, spaceUrl);
        }
    }
}
