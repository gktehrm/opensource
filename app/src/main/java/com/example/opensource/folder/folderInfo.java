package com.example.opensource.folder;

public class folderInfo {
    private String id;
    private String name; // 폴더 이름
    private String lastModified;  // 마지막 수정 날짜


    public folderInfo(String name, String lastModified) {
        this.name = name;
        this.lastModified = lastModified;

    }

    public folderInfo(String id, String name, String lastModified) {
        this.name = name;
        this.lastModified = lastModified;
        this.id = id;
    }

    // 🔹 ID Getter/Setter
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    // 🔹 Title Getter/Setter
    public String getname() {
        return name;
    }
    public void setname(String name) {
        this.name = name;
    }


    // 🔹 Date Getter
    public String getlastModified() {
        return lastModified;
    }
    public void setlastModified(String lastModified) {
        this.lastModified =lastModified;
    }



}
