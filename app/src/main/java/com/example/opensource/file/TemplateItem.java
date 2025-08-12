package com.example.opensource.file;

class TemplateItem {
    private String fileName;
    private String fileType;
    private String filePath;
    private int previewImageResId;

    public TemplateItem(String fileName, String fileType, String filePath, int previewImageResId) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.previewImageResId = previewImageResId;
    }

    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getFilePath() { return filePath; }
    public int getPreviewImageResId() { return previewImageResId; }
}