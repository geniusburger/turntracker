package me.geniusburger.turntracker.api;

import java.util.List;

import me.geniusburger.turntracker.api.model.NotificationRequest;
import me.geniusburger.turntracker.api.model.TakeTurnRequest;
import me.geniusburger.turntracker.api.model.TakeTurnResponse;
import me.geniusburger.turntracker.api.model.UpdateTokenRequest;
import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface TurnTrackerService {

    @GET("user")
    Call<User> getUser(@Query("username") String username);

    @PUT("android")
    boolean updateToken(@Body UpdateTokenRequest updateTokenRequest);

    @DELETE("android")
    boolean deleteToken(@Query("user_id") long userId);

    @DELETE("turn")
    boolean deleteTurn(@Query("turn_id") long turnId, @Query("task_id") long taskId, @Query("user_id") long userId);

    @GET("taskusers")
    Call<List<User>> getTaskUsers(@Query("id") long taskId);

    @POST("turn")
    Call<TakeTurnResponse> takeTurn(@Body TakeTurnRequest request);

    @PUT("subscription")
    Call<Boolean> setSubscription(@Body NotificationRequest notification);

    @DELETE("subscription")
    Call<Boolean> deleteSubscription(@Query("task_id") long taskId, @Query("user_id") long userId);
}
