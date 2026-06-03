package ttv.migami.jeg.item.attachment;

public enum AttachmentType {
    SCOPE("scope"),
    BARREL("barrel"),
    STOCK("stock"),
    UNDER_BARREL("under_barrel"),
    MAGAZINE("magazine"),
    SPECIAL("special");

    private final String key;

    AttachmentType(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }
}
