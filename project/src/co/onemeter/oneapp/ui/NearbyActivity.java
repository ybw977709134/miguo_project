package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.LocationHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NearbyActivity extends Activity implements OnClickListener {
	private class PersonListAdapter extends BaseAdapter {

		private Context context;
        private ArrayList<Buddy> buddies;
        private final int SEC_IN_MINUTE = 60;
        private final int SEC_IN_HOUR = 60 * 60;
        private final int SEC_IN_DAY = 60 * 60 * 24;
        private Date date;

		public PersonListAdapter(Context context, ArrayList<Buddy> buddies) {
			this.context = context;
            this.buddies = buddies;
            try {
                date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse("1970/01/01 08:00:00");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        public void setDataSource(ArrayList<Buddy> buddies) {
            this.buddies = buddies;
        }

        public ArrayList<Buddy> getDataSource() {
            return this.buddies;
        }

		@Override
		public int getCount() {
			return buddies.size();
		}

		@Override
		public Object getItem(int position) {
			return buddies.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			Buddy buddy = buddies.get(position);
			if (convertView == null) {
                holder = new ViewHolder();
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_nearby_personlist, null);
				holder.imgPhoto = (ImageView) convertView.findViewById(R.id.img_photo);
				holder.txtName = (TextView) convertView.findViewById(R.id.txt_name);
				holder.txtStatus = (TextView) convertView.findViewById(R.id.txt_status);
				holder.txtAge = (TextView) convertView.findViewById(R.id.txt_age);
				holder.txtDistance = (TextView) convertView.findViewById(R.id.txt_distance);
				holder.txtTime = (TextView) convertView.findViewById(R.id.txt_time);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
            PhotoDisplayHelper.displayPhoto(NearbyActivity.this, holder.imgPhoto, R.drawable.default_avatar_90, buddy, true);
			holder.txtName.setText(buddy.nickName);
			holder.txtStatus.setText(buddy.status);
            if (buddy.getSexFlag() == Buddy.SEX_FEMALE) {
                holder.txtAge.setBackgroundResource(R.drawable.female_age_bg);
            } else if (buddy.getSexFlag() == Buddy.SEX_MALE) {
                holder.txtAge.setBackgroundResource(R.drawable.male_age_bg);
            } else {
                holder.txtAge.setBackgroundResource(R.drawable.unsex_age_bg);
            }
            float distance[] = new float[1];
            Location.distanceBetween(buddy.lastLocation.latitude, buddy.lastLocation.longitude, mLocation.getLatitude(), mLocation.getLongitude(), distance);
            float dis = distance[0] / 1000;
            if (dis < 10) {
                holder.txtDistance.setText(String.format("%1$.2fkm", dis));
            } else if (dis < 100) {
                holder.txtDistance.setText(String.format("%1$.1fkm", dis));
            } else {
                holder.txtDistance.setText(String.format("%1$.0fkm", dis));
            }
            calculateTime(holder.txtTime, buddy.lastOnline.getTime() * 1000);
            calculateAge(holder.txtAge, buddy.getBirthday());
			return convertView;
		}
		
		class ViewHolder {
			ImageView imgPhoto;
			TextView txtName;
			TextView txtStatus;
			TextView txtAge;
			TextView txtDistance;
			TextView txtTime;
		}
	}
	
	private class PersonGridAdapter extends BaseAdapter {
		private Context context;
        private ArrayList<Buddy> buddies;
		public PersonGridAdapter(Context context, ArrayList<Buddy> buddies) {
			this.context = context;
            this.buddies = buddies;
		}

        public void setDataSource(ArrayList<Buddy> buddies) {
            this.buddies = buddies;
        }

        public ArrayList<Buddy> getDataSource() {
            return this.buddies;
        }

		@Override
		public int getCount() {
			return buddies.size();
		}

		@Override
		public Object getItem(int position) {
			return buddies.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final Buddy buddy = buddies.get(position);
			if (convertView == null) {
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_nearby_persongrid, null);
			}
            convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContactInfoActivity.launch(NearbyActivity.this, Person.fromBuddy(buddy), ContactInfoActivity.BUDDY_TYPE_UNKNOWN);
                }
            });
			ImageView imgPhoto = (ImageView) convertView.findViewById(R.id.img_photo);
			ImageView imgSex = (ImageView) convertView.findViewById(R.id.img_sex);
			TextView txtDistance = (TextView) convertView.findViewById(R.id.txt_distance);
            PhotoDisplayHelper.displayPhoto(NearbyActivity.this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
			if (buddy.getSexFlag() == Buddy.SEX_FEMALE) {
				imgSex.setBackgroundResource(R.drawable.female_left_bg);
			} else if (buddy.getSexFlag() == Buddy.SEX_MALE) {
				imgSex.setBackgroundResource(R.drawable.male_left_bg);
			} else {
                imgSex.setBackgroundResource(R.drawable.unsex_left_bg);
            }
            float[] distance = new float[1];
            Location.distanceBetween(buddy.lastLocation.latitude, buddy.lastLocation.longitude, mLocation.getLatitude(), mLocation.getLongitude(), distance);
            float dis = distance[0] / 1000;
            if (dis < 10) {
                txtDistance.setText(String.format("%1$.2fkm", dis));
            } else if (dis < 100) {
                txtDistance.setText(String.format("%1$.1fkm", dis));
            } else {
                txtDistance.setText(String.format("%1$.0fkm", dis));
            }
			return convertView;
		}
	}

    private class GroupRoomAdapter extends BaseAdapter {

        private Context context;
        public GroupRoomAdapter(Context context) {
            this.context = context;
        }
        @Override
        public int getCount() {
            return groups.size();  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object getItem(int position) {
            return groups.get(position);  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public long getItemId(int position) {
            return position;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GroupChatRoom group = groups.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.listitem_nearby_group, parent, false);
            }
            ImageView imgPhoto = (ImageView) convertView.findViewById(R.id.img_photo);
            PhotoDisplayHelper.displayPhoto(NearbyActivity.this, imgPhoto, R.drawable.default_avatar_90, group, true);
            TextView txtGroupName = (TextView) convertView.findViewById(R.id.txt_group_name);
            TextView txtGroupStatus = (TextView) convertView.findViewById(R.id.txt_group_status);
            TextView txtMemberCount = (TextView) convertView.findViewById(R.id.txt_member_count);
            txtGroupName.setText(group.getDisplayName());
            txtGroupStatus.setText(group.groupStatus);
            txtMemberCount.setText(String.format("", group.memberCount));
            return convertView;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    private static final int REQ_OPEN_GPS = 10;

	private ImageButton btnTitleBack;
	private ImageButton btnTitleViewType;
	private Button btnPerson;
	private Button btnGroup;
	private LinearLayout mRightLayout;

    private FrameLayout personFrame;
    private FrameLayout groupFrame;

    private ListView personList;
    private GridView personGrid;

    private MessageBox mMsgBox;
    private BottomButtonBoard mBoard;

	private boolean _is_show_group = false;
	private boolean _is_list_view = true;
	
	private ArrayList<Buddy> buddies;
    private ArrayList<Buddy> maleBuddies;
    private ArrayList<Buddy> femaleBuddies;
    private ArrayList<GroupChatRoom> groups;

    private PersonListAdapter personListAdapter;
    private PersonGridAdapter personGridAdapter;
    private GroupRoomAdapter groupRoomAdapter;
    private Location mLocation;

    private LocationHelper locationHelper;

    private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleViewType = (ImageButton) findViewById(R.id.title_viewtype);
		btnPerson = (Button) findViewById(R.id.title_person);
		btnGroup = (Button) findViewById(R.id.title_group);
		mRightLayout = (LinearLayout) findViewById(R.id.right_layout);
        personFrame = (FrameLayout) findViewById(R.id.person_frame);
        groupFrame = (FrameLayout) findViewById(R.id.group_frame);
        personList = (ListView) findViewById(R.id.person_list);
        personGrid = (GridView) findViewById(R.id.person_grid);

        personGrid.setColumnWidth(org.wowtalk.ui.GlobalValue.screenW / 4);
		
		btnTitleBack.setOnClickListener(this);
		btnTitleViewType.setOnClickListener(this);
		btnPerson.setOnClickListener(this);
		btnGroup.setOnClickListener(this);
		if (_is_show_group) {
			changeToGroupView();
		} else {
			changeToPersonView();
		}
        if (_is_list_view) {
//            btnTitleViewType.setImageResource(R.drawable.nav_list_view);
        } else {
//            btnTitleViewType.setImageResource(R.drawable.nav_image_view);
        }
        personList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContactInfoActivity.launch(NearbyActivity.this,
                        Person.fromBuddy(personListAdapter.getDataSource().get(position)),
                        ContactInfoActivity.BUDDY_TYPE_UNKNOWN);
            }
        });
        personGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContactInfoActivity.launch(NearbyActivity.this,
                        Person.fromBuddy(personGridAdapter.getDataSource().get(position)),
                        ContactInfoActivity.BUDDY_TYPE_UNKNOWN);
            }
        });
	}

    private void calculateTime(TextView txtTime, long timestamp) {
        long newTimestamp = new Date().getTime();
        long timePeriod = (newTimestamp - timestamp) / 1000;
        if (timePeriod < 60) {
            txtTime.setText(String.format(
                    NearbyActivity.this.getResources().getString(R.string.minutes_ago), 0));
        } else if (timePeriod < 60 * 60) {
            txtTime.setText(String.format(
                    NearbyActivity.this.getResources().getString(R.string.minutes_ago), timePeriod / 60));
        } else if (timePeriod < 60 * 60 * 24) {
            txtTime.setText(String.format(
                    NearbyActivity.this.getResources().getString(R.string.hours_ago), timePeriod / (60 * 60)));
        } else {
            txtTime.setText(String.format(
                    NearbyActivity.this.getResources().getString(R.string.days_ago), timePeriod / (24 * 60 * 60)));
        }
    }

    private void calculateAge(TextView txtAge, Date birthday) {
        int age = 0;
        if (birthday != null) {
            Date now = new Date();
            if (now.getYear() - birthday.getYear() > 100 || now.getYear() - birthday.getYear() < 0) {
                txtAge.setText("");
                return;
            }
            if ((birthday.getDay() > now.getDay() && birthday.getMonth() == now.getMonth())
                    || birthday.getMonth() > now.getMonth()) {
                age = now.getYear() - birthday.getYear() - 1;
            } else {
                age = now.getYear() - birthday.getYear();
            }
        }
        if (age > 0)
            txtAge.setText(Integer.toString(age));
        else
            txtAge.setText("");
    }
	
	private void changeToPersonView() {
//		btnPerson.setBackgroundResource(R.drawable.tab_button_left_a);
		btnGroup.setBackgroundResource(R.drawable.tab_button_right);
		btnPerson.setTextColor(getResources().getColor(R.color.white));
		btnGroup.setTextColor(getResources().getColor(R.color.gray));
		mRightLayout.setVisibility(View.VISIBLE);
		if (_is_show_group) {
			_is_show_group = false;
			personFrame.setVisibility(View.VISIBLE);
            groupFrame.setVisibility(View.GONE);
		}
        if (_is_list_view) {
            personList.setVisibility(View.VISIBLE);
            personGrid.setVisibility(View.GONE);
        } else {
            personGrid.setVisibility(View.VISIBLE);
            personList.setVisibility(View.GONE);
        }
	}
	
	private void changeToGroupView() {
		btnPerson.setBackgroundResource(R.drawable.tab_button_left);
		btnGroup.setBackgroundResource(R.drawable.tab_button_right_a);
		btnPerson.setTextColor(getResources().getColor(R.color.gray));
		btnGroup.setTextColor(getResources().getColor(R.color.white));
		mRightLayout.setVisibility(View.GONE);
		if (_is_show_group)
			return;
		_is_show_group = true;
		groupFrame.setVisibility(View.VISIBLE);
        personFrame.setVisibility(View.GONE);
	}
	
	private void changeViewType() {
		if (_is_list_view) {
            PrefUtil.getInstance(this).setNearbyResultLayout(PrefUtil.GRID_LAYOUT);
//            btnTitleViewType.setImageResource(R.drawable.nav_image_view);
            personList.setVisibility(View.GONE);
            personGrid.setVisibility(View.VISIBLE);
		} else {
            PrefUtil.getInstance(this).setNearbyResultLayout(PrefUtil.LIST_LAYOUT);
//			btnTitleViewType.setImageResource(R.drawable.nav_list_view);
            personList.setVisibility(View.VISIBLE);
            personGrid.setVisibility(View.GONE);
		}
        _is_list_view = !_is_list_view;
	}

    private void showActionSheet() {
        mBoard.clearView();
        mBoard.add(getString(R.string.moments_see_all), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBoard.dismiss();
                        if (buddies != null) {
                            personListAdapter.setDataSource(buddies);
                            personGridAdapter.setDataSource(buddies);
                            personListAdapter.notifyDataSetChanged();
                            personGridAdapter.notifyDataSetChanged();
                        }
                    }
                });
        mBoard.add(getString(R.string.moments_only_male), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBoard.dismiss();
                        if (maleBuddies != null) {
                            personListAdapter.setDataSource(maleBuddies);
                            personGridAdapter.setDataSource(maleBuddies);
                            personListAdapter.notifyDataSetChanged();
                            personGridAdapter.notifyDataSetChanged();
                        }
                    }
                });
        mBoard.add(getString(R.string.moments_only_female), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBoard.dismiss();
                        if (femaleBuddies != null) {
                            personListAdapter.setDataSource(femaleBuddies);
                            personGridAdapter.setDataSource(femaleBuddies);
                            personListAdapter.notifyDataSetChanged();
                            personGridAdapter.notifyDataSetChanged();
                        }
                    }
                });
        final String viewType = _is_list_view ?
                getString(R.string.moments_view_type_grid) : getString(R.string.moments_view_type_list);
        mBoard.add(viewType, BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeViewType();
                        mBoard.dismiss();
                    }
                });
        mBoard.addCancelBtn(getString(R.string.close));
        mBoard.show();
    }
	
	private void fetchNearbyPerson(final double latitude, final double longitude) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                buddies = new ArrayList<Buddy>();
                return WebServerIF.getInstance(NearbyActivity.this).fGetBuddiesNearby(false, latitude, longitude, buddies);
            }
            @Override
            protected void onPostExecute(Integer result) {
                if (result == ErrorCode.OK) {
                    maleBuddies = new ArrayList<Buddy>();
                    femaleBuddies = new ArrayList<Buddy>();
                    for (Buddy b : buddies) {
                        if (b.getSexFlag() == Buddy.SEX_MALE) {
                            maleBuddies.add(b);
                        } else if (b.getSexFlag() == Buddy.SEX_FEMALE) {
                            femaleBuddies.add(b);
                        }
                    }
                    personListAdapter = new PersonListAdapter(NearbyActivity.this, buddies);
                    personGridAdapter = new PersonGridAdapter(NearbyActivity.this, buddies);
                    personList.setAdapter(personListAdapter);
                    personGrid.setAdapter(personGridAdapter);
                    mMsgBox.dismissWait();
                }

            }
        }.execute((Void)null);

	}

    private void updateMyLocation() {
        mMsgBox.showWait();
       new AsyncTask<Void, Void, Integer>() {

           @Override
           protected Integer doInBackground(Void... params) {
               Buddy buddy = new Buddy();
               buddy.lastLocation = new WLocation();
               buddy.lastLocation.latitude = mLocation.getLatitude();
               buddy.lastLocation.longitude = mLocation.getLongitude();
               return WebServerIF.getInstance(NearbyActivity.this).fUpdateMyProfile(buddy, Buddy.FIELD_FLAG_SPOT);
           }
           @Override
           protected void onPostExecute(Integer result) {
               if (result == ErrorCode.OK) {
                   fetchNearbyGroup(mLocation.getLatitude(), mLocation.getLongitude());
                   fetchNearbyPerson(mLocation.getLatitude(), mLocation.getLongitude());
                } else {
                   mMsgBox.dismissWait();
               }
           }
       }.execute((Void)null);
    }

    private void fetchNearbyGroup(final double latitude, final double longitude) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                groups = new ArrayList<GroupChatRoom>();
                return WebServerIF.getInstance(NearbyActivity.this).fGroupChat_GetNearBy(false, latitude, longitude, groups);
            }
            @Override
            protected void onPostExecute(Integer result) {
                if (result == ErrorCode.OK) {
                    groupRoomAdapter = new GroupRoomAdapter(NearbyActivity.this);
                }
            }
        }.execute((Void)null);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.title_back:
                finish();
                break;
        case R.id.title_viewtype:
//                changeViewType();
            showActionSheet();
                break;
        case R.id.title_person:
			changeToPersonView();
			break;
		case R.id.title_group:
			changeToGroupView();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nearby_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

//        if (!isGPSAvailable() && !isNetWorkAvailable()) {
//            Toast.makeText(this, "当前位置信息不可用！", Toast.LENGTH_LONG).show();
//        } else {
//            if (isGPSAvailable()) {
//                getLocationByGPS();
//            } else {
//                getLocationByNetwork();
//            }
//        }
        mMsgBox = new MessageBox(this);
        mBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        locationHelper = new LocationHelper(this);
        locationHelper.setOnLocationGotListener(new LocationHelper.OnLocationGotListener() {
            @Override
            public void onLocationGot(Location location, String strAddress) {
                mLocation = location;
                updateMyLocation();
            }

            @Override
            public void onNoLocationGot() {
                mMsgBox.toast(R.string.cannot_get_location_infomation);
            }
        });
        locationHelper.getLocationWithAMap(false);
        if (PrefUtil.getInstance(this).getNearbyResultLayout() == PrefUtil.LIST_LAYOUT) {
            _is_list_view = true;
        } else {
            _is_list_view = false;
        }
        initView();
	}

    @Override
    protected void onResume() {
        super.onPause();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
