package com.example.baicuoiky04;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public final class DataModels {

    private DataModels() {}

    public static class User {
        private String uid;
        private String displayName;
        private String email;
        private String photoUrl;
        private String bio;
        private String contactInfo;
        private double averageRating;
        private long totalReviews;
        private long totalTransactions;
        private List<String> savedListings;
        private List<String> blockedUsers;
        private String accountStatus;
        private String role;
        private String fcmToken;
        @ServerTimestamp
        private Date createdAt;

        public User() {}

        public String getUid() { return uid; }
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
        public void setUid(String uid) { this.uid = uid; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getContactInfo() { return contactInfo; }
        public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        public long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }
        public long getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
        public List<String> getSavedListings() { return savedListings; }
        public void setSavedListings(List<String> savedListings) { this.savedListings = savedListings; }
        public List<String> getBlockedUsers() { return blockedUsers; }
        public void setBlockedUsers(List<String> blockedUsers) { this.blockedUsers = blockedUsers; }
        public String getAccountStatus() { return accountStatus; }
        public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class Report {
        private String reporterId;
        private String reportedUserId;
        private String reportedListingId;
        private String reportedReviewId;
        private String reason;
        private String comment;
        @ServerTimestamp
        private Date createdAt;
        @Exclude
        private User reportedUserObject;

        public Report() {}

        public String getReporterId() { return reporterId; }
        public void setReporterId(String reporterId) { this.reporterId = reporterId; }
        public String getReportedUserId() { return reportedUserId; }
        public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }
        public String getReportedListingId() { return reportedListingId; }
        public void setReportedListingId(String reportedListingId) { this.reportedListingId = reportedListingId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        public String getReportedReviewId() { return reportedReviewId; }
        public void setReportedReviewId(String reportedReviewId) { this.reportedReviewId = reportedReviewId; }
        @Exclude
        public User getReportedUserObject() { return reportedUserObject; }
        public void setReportedUserObject(User reportedUserObject) { this.reportedUserObject = reportedUserObject; }
    }

    public static class Listing {
        private String listingId;
        private String sellerId;
        private String sellerName;
        private String sellerPhotoUrl;
        private String title;
        private String description;
        private long price;
        private boolean isNegotiable;
        private String category;
        private String condition;
        private String locationName;
        private GeoPoint locationGeoPoint;
        private List<String> imageUrls;
        private List<String> tags;
        private String status;
        private long views;
        private long offersCount;
        private String buyerId;
        @ServerTimestamp
        private Date createdAt;
        @ServerTimestamp
        private Date lastUpdatedAt;
        @Exclude
        private float distanceToUser = -1;

        public Listing() {}

        public String getListingId() { return listingId; }
        public void setListingId(String listingId) { this.listingId = listingId; }
        public String getSellerId() { return sellerId; }
        public void setSellerId(String sellerId) { this.sellerId = sellerId; }
        public String getSellerName() { return sellerName; }
        public void setSellerName(String sellerName) { this.sellerName = sellerName; }
        public String getSellerPhotoUrl() { return sellerPhotoUrl; }
        public void setSellerPhotoUrl(String sellerPhotoUrl) { this.sellerPhotoUrl = sellerPhotoUrl; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public long getPrice() { return price; }
        public void setPrice(long price) { this.price = price; }
        public boolean isNegotiable() { return isNegotiable; }
        public void setNegotiable(boolean negotiable) { this.isNegotiable = negotiable; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }
        public GeoPoint getLocationGeoPoint() { return locationGeoPoint; }
        public void setLocationGeoPoint(GeoPoint locationGeoPoint) { this.locationGeoPoint = locationGeoPoint; }
        public List<String> getImageUrls() { return imageUrls; }
        public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getViews() { return views; }
        public void setViews(long views) { this.views = views; }
        public long getOffersCount() { return offersCount; }
        public void setOffersCount(long offersCount) { this.offersCount = offersCount; }
        public String getBuyerId() { return buyerId; }
        public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        public Date getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(Date lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
        @Exclude
        public float getDistanceToUser() { return distanceToUser; }
        public void setDistanceToUser(float distanceToUser) { this.distanceToUser = distanceToUser; }
    }

    public static class Review {
        @Exclude
        private String id;
        private String listingId;
        private String reviewerId;
        private String reviewerName;
        private String reviewerPhotoUrl;
        private double rating;
        private String comment;
        @ServerTimestamp
        private Date createdAt;
        private String status = "visible";
        public Review() {}
        public String getListingId() { return listingId; }
        public void setListingId(String listingId) { this.listingId = listingId; }
        public String getReviewerId() { return reviewerId; }
        public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
        public String getReviewerName() { return reviewerName; }
        public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
        public String getReviewerPhotoUrl() { return reviewerPhotoUrl; }
        public void setReviewerPhotoUrl(String reviewerPhotoUrl) { this.reviewerPhotoUrl = reviewerPhotoUrl; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        @Exclude
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class Offer {
        private String buyerId;
        private String buyerName;
        private long offerPrice;
        private String status;
        @ServerTimestamp
        private Date createdAt;
        public Offer() {}
        public String getBuyerId() { return buyerId; }
        public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
        public String getBuyerName() { return buyerName; }
        public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
        public long getOfferPrice() { return offerPrice; }
        public void setOfferPrice(long offerPrice) { this.offerPrice = offerPrice; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    }

    public static class HomeFeedItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_HORIZONTAL_LIST = 1;
        public static final int TYPE_GRID_LISTING = 2;
        public int type;
        public String headerTitle;
        public List<Listing> listings;
        public Listing singleListing;
        public HomeFeedItem(int type, String headerTitle) { this.type = type; this.headerTitle = headerTitle; }
        public HomeFeedItem(int type, List<Listing> listings) { this.type = type; this.listings = listings; }
        public HomeFeedItem(int type, Listing listing) { this.type = type; this.singleListing = listing; }
    }

    public static class OfferWithListing {
        private Offer offer;
        private Listing listing;
        private String offerId;
        public OfferWithListing(Offer offer, Listing listing, String offerId) {
            this.offer = offer;
            this.listing = listing;
            this.offerId = offerId;
        }
        public Offer getOffer() { return offer; }
        public Listing getListing() { return listing; }
        public String getOfferId() { return offerId; }
    }

    public static class Chat {
        private List<String> participants;
        private String lastMessage;
        @ServerTimestamp
        private Date lastMessageTimestamp;
        private String user1Id;
        private String user1Name;
        private String user1Photo;
        private String user2Id;
        private String user2Name;
        private String user2Photo;
        private String status;
        public Chat() { this.status = "active"; }
        public List<String> getParticipants() { return participants; }
        public void setParticipants(List<String> participants) { this.participants = participants; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
        public Date getLastMessageTimestamp() { return lastMessageTimestamp; }
        public void setLastMessageTimestamp(Date lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }
        public String getUser1Id() { return user1Id; }
        public void setUser1Id(String user1Id) { this.user1Id = user1Id; }
        public String getUser1Name() { return user1Name; }
        public void setUser1Name(String user1Name) { this.user1Name = user1Name; }
        public String getUser1Photo() { return user1Photo; }
        public void setUser1Photo(String user1Photo) { this.user1Photo = user1Photo; }
        public String getUser2Id() { return user2Id; }
        public void setUser2Id(String user2Id) { this.user2Id = user2Id; }
        public String getUser2Name() { return user2Name; }
        public void setUser2Name(String user2Name) { this.user2Name = user2Name; }
        public String getUser2Photo() { return user2Photo; }
        public void setUser2Photo(String user2Photo) { this.user2Photo = user2Photo; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class Message {
        private String senderId;
        private String text;
        private String imageUrl;
        @ServerTimestamp
        private Date timestamp;
        public Message() {}
        public Message(String senderId, String text) {
            this.senderId = senderId;
            this.text = text;
        }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    public static class AppNotification {
        private String userId; // ID của người sẽ nhận thông báo
        private String title;
        private String body;
        private boolean isRead = false;
        @ServerTimestamp
        private Date createdAt;

        // Các trường tùy chọn để điều hướng khi người dùng bấm vào
        private String listingId;
        private String chatId;
        private String senderId;

        public AppNotification() {}

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        public String getListingId() { return listingId; }
        public void setListingId(String listingId) { this.listingId = listingId; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
    }
}