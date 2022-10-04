package JsonUtils;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ObstacleJson {
    @SerializedName("areas")
    private List<Boolean> areas;

    public List<Boolean> getAreas(){
        return areas;
    }
}
