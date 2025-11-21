package fpl.manhph61584.duan1_nhom3_app.network;

import java.util.List;

import fpl.manhph61584.duan1_nhom3_app.Product;
import fpl.manhph61584.duan1_nhom3_app.network.dto.LoginRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.LoginResponse;
import fpl.manhph61584.duan1_nhom3_app.network.dto.RegisterRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.RegisterResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Auth endpoints
    @POST("users/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("users/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // Product endpoints
    @GET("products")
    Call<List<Product>> getProducts(@Query("search") String search);

    @GET("products/{id}")
    Call<Product> getProductDetail(@Path("id") String id);
}

