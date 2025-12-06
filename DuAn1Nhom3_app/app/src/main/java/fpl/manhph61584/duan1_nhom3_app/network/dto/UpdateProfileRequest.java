package fpl.manhph61584.duan1_nhom3_app.network.dto;

public class UpdateProfileRequest {
    private String name;
    private String avatar;

    public UpdateProfileRequest(String name, String avatar) {
        this.name = name;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}



















