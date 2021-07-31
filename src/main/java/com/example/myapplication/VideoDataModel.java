package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoDataModel implements Serializable {
    public String video;
    public String description;
    public Count count;
    public UserInfo user_info;
    private float normalisedCount = 0;
    //public String _cacheKey;

    /*public void setCacheKey(String _cacheKey) {
        this._cacheKey = _cacheKey;
    }*/

    public String getNormalisedCount() {
        return CommonUtil.humanReadableByteCountSI(count.like_count);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Count implements Serializable {
        public Double like_count;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo implements Serializable {
        public String username;
        public String profile_pic;
    }


    @Nullable
    public static ArrayList<VideoDataModel> getListFromString(@NonNull String resource) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<VideoDataModel> list = null;
        if (resource.length() > 0) {
            try {
                objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
                list = objectMapper.readValue(resource, objectMapper.getTypeFactory().constructCollectionType(List.class, VideoDataModel.class));
                //list = objectMapper.readValue(resource, new ArrayList<VideoDataModel>().getClass());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
