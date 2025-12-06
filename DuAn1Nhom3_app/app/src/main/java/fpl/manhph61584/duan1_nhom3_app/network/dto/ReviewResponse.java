package fpl.manhph61584.duan1_nhom3_app.network.dto;

import fpl.manhph61584.duan1_nhom3_app.Review;

public class ReviewResponse {
    private String message;
    private Review review;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }
}

