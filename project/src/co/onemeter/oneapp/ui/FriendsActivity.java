package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Database;
import org.wowtalk.api.IDBTableChangeListener;
import org.wowtalk.api.Moment;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

public class FriendsActivity extends Activity implements OnClickListener{
	private TextView mTrends;
	private TextView mAddFriend;
    private TextView mPublicSearch;
    private TextView mRecommendedPublic;
	private TextView mAround;
	private TextView mInterest;
    private TextView mMomentsNewsIndicator;
    private MessageBox mMsgBox;
    private static FriendsActivity _instance;

	private void initView() {
		mTrends = (TextView) findViewById(R.id.trends);
		mAddFriend = (TextView) findViewById(R.id.add_friend);
        mPublicSearch = (TextView) findViewById(R.id.public_search);
        mRecommendedPublic = (TextView) findViewById(R.id.recommended_public);
		mAround = (TextView) findViewById(R.id.around);
		mInterest = (TextView) findViewById(R.id.interest);
        mMomentsNewsIndicator = (TextView)findViewById(R.id.txt_moments_news_indicator);
		
		mTrends.setOnClickListener(this);
		mAddFriend.setOnClickListener(this);
        mPublicSearch.setOnClickListener(this);
        mRecommendedPublic.setOnClickListener(this);
		mAround.setOnClickListener(this);
		mInterest.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.trends:
            if (!Environment.getExternalStorageDirectory().exists()) {
                mMsgBox.toast(R.string.no_sdcard_tips);
            } else {
                Intent trendIntent = new Intent(FriendsActivity.this, MomentActivity.class);
                startActivity(trendIntent);
            }
			break;
		case R.id.add_friend:
			Intent addFriendIntent = new Intent(FriendsActivity.this, ContactAddActivity.class);
			startActivity(addFriendIntent);
			break;
        case R.id.public_search:
            Intent publicIntent = new Intent(FriendsActivity.this, PublicSearchActivity.class);
            startActivity(publicIntent);
            break;
        case R.id.recommended_public:
            Intent recommendIntent = new Intent(FriendsActivity.this, PublicSearchActivity.class);
            startActivity(recommendIntent);
            break;
		case R.id.around:
			Intent aroundIntent = new Intent(FriendsActivity.this, NearbyActivity.class);
			startActivity(aroundIntent);
			break;
		case R.id.interest:
			mMsgBox.toast(R.string.not_implemented);
			break;
		default:
			mMsgBox.toast(R.string.not_implemented);
			break;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friends);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
		initView();
	}

    private IDBTableChangeListener momentReviewObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshBadge();
                }
            });
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        _instance = this;
        MobclickAgent.onResume(this);
        refreshBadge();

        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentReviewObserver);
//        StartActivity.instance().fRefreshTabBadge_social();
    }

    public void refreshBadge() {
        Moment dummy = new Moment(null);
        int newReviewsCount=new Database(this).fetchNewReviews(dummy);

        if (0 == newReviewsCount) {
            mMomentsNewsIndicator.setText("");
            mMomentsNewsIndicator.setVisibility(View.GONE);
        } else {
            mMomentsNewsIndicator.setText(String.valueOf(newReviewsCount));
            mMomentsNewsIndicator.setVisibility(View.VISIBLE);
        }

//        if (GlobalValue.unreadMomentReviews == 0) {
//            mMomentsNewsIndicator.setText("");
//            mMomentsNewsIndicator.setVisibility(View.GONE);
//        } else {
//            mMomentsNewsIndicator.setText(String
//                    .valueOf(GlobalValue.unreadMomentReviews));
//            mMomentsNewsIndicator.setVisibility(View.VISIBLE);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        _instance = null;

        Database.removeDBTableChangeListener(momentReviewObserver);
    }

    public static FriendsActivity instance() {
        return _instance;
    }
}
