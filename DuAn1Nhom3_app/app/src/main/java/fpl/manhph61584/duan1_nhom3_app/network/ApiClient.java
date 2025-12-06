package fpl.manhph61584.duan1_nhom3_app.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private static Retrofit retrofit;

    private ApiClient() {
        // Private constructor
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            // Tạo Gson với lenient mode để xử lý JSON linh hoạt hơn
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}







