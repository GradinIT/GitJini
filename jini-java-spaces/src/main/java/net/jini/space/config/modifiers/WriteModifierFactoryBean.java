package net.jini.space.config.modifiers;

/**
 * FactoryBean for creating a WriteModifier.
 */
public class WriteModifierFactoryBean {

    private String modifierName;

    public void setModifierName(String modifierName) {
        this.modifierName = modifierName;
    }

    public WriteModifier getObject() {
        return WriteModifier.valueOf(modifierName);
    }
}
