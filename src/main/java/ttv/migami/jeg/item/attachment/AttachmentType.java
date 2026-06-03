package ttv.migami.jeg.item.attachment;

public enum AttachmentType {
    SCOPE("scope"),
    BARREL("barrel"),
    STOCK("stock"),
    UNDER_BARREL("under_barrel"),
    MAGAZINE("magazine"),
    SPECIAL("special"),
    PAINT_JOB("paint_job"),
    DYE("dye"),
    KILL_EFFECT("kill_effect");

    private final String key;

    AttachmentType(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public boolean isCosmetic() {
        return this == PAINT_JOB || this == DYE || this == KILL_EFFECT;
    }
}
