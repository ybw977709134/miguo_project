package org.wowtalk.api;

import java.util.Iterator;

/**
 * Holds a Review list, and the list is stored in local db.
 * User: pan
 * Date: 4/20/13
 * Time: 2:01 PM
 */
public interface IHasReview {
    public String getReviewDataTableName();
    public String getReviewDataTablePrimaryKeyName();
    public String getReviewDataTablePrimaryKeyValue();
    public int getReviewsCount();
    public Iterator<Review> getReviewIterator();
    public void addReview(Review review);
    public void clearReviews();
    public String getOwnerUid();
}
