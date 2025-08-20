package com.example.opensource.repository;

/**
 * 저장소(폴더) 정보를 담는 엔티티 클래스
 */
public class RepositoryInfo {
    private String id;
    private String name;
    private String lastModified;

    public RepositoryInfo() {}

    public RepositoryInfo(String name, String lastModified) {
        this.name = name;
        this.lastModified = lastModified;
    }

    public RepositoryInfo(String id, String name, String lastModified) {
        this.id = id;
        this.name = name;
        this.lastModified = lastModified;
    }

    /** @return 저장소 ID */
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    /** @return 폴더 이름 */
    public String getname() { return name; }

    public void setname(String name) { this.name = name; }

    /** @return 마지막 수정 날짜 */
    public String getlastModified() { return lastModified; }

    public void setlastModified(String lastModified) { this.lastModified = lastModified; }
}
