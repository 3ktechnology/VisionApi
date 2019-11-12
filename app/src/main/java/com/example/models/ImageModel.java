package com.example.models;


import android.media.Image;

import com.google.gson.annotations.SerializedName;

public class ImageModel {
    @SerializedName("image")
    private ImageDetails image;
    @SerializedName("rotation")
    private ImageRotation rotation;

    public ImageModel() {
    }

    public ImageModel(ImageDetails image, ImageRotation rotation) {
        this.image = image;
        this.rotation = rotation;
    }

    public ImageDetails getImage() {
        return image;
    }

    public void setImage(ImageDetails image) {
        this.image = image;
    }

    public ImageRotation getRotation() {
        return rotation;
    }

    public void setRotation(ImageRotation rotation) {
        this.rotation = rotation;
    }

    public static class ImageRotation {

        @SerializedName("accuracy")
        private String accuracy;
        @SerializedName("confidence")
        private String confidence;
        @SerializedName("rotation")
        private String rotation;

        public ImageRotation() {
        }

        public ImageRotation(String accuracy, String confidence, String rotation) {
            this.accuracy = accuracy;
            this.confidence = confidence;
            this.rotation = rotation;
        }

        public String getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(String accuracy) {
            this.accuracy = accuracy;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getRotation() {
            return rotation;
        }

        public void setRotation(String rotation) {
            this.rotation = rotation;
        }
    }

    public static class ImageDetails {

        @SerializedName("image_base64")
        private String image_base64;
        @SerializedName("s3_bucket")
        private String s3_bucket;
        @SerializedName("s3_key")
        private String s3_key;

        public ImageDetails() {
        }

        public ImageDetails(String image_base64, String s3_bucket, String s3_key) {
            this.image_base64 = image_base64;
            this.s3_bucket = s3_bucket;
            this.s3_key = s3_key;
        }

        public String getImage_base64() {
            return image_base64;
        }

        public void setImage_base64(String image_base64) {
            this.image_base64 = image_base64;
        }

        public String getS3_bucket() {
            return s3_bucket;
        }

        public void setS3_bucket(String s3_bucket) {
            this.s3_bucket = s3_bucket;
        }

        public String getS3_key() {
            return s3_key;
        }

        public void setS3_key(String s3_key) {
            this.s3_key = s3_key;
        }
    }
}
