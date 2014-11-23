package co.onemeter.oneapp.contacts.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.Utils;
import co.onemeter.oneapp.contacts.model.Group;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.ui.Log;

import java.util.ArrayList;
import java.util.Locale;

public class ContactUtil {
	private static String[] groupName = new String[] {"ȫ����ϵ��", "�绰����ϵ��", "Ⱥ��", "����"};
	
	public static ArrayList<Person> allPersons = new ArrayList<Person>();
	public static ArrayList<Person> curPersons = new ArrayList<Person>();
	public static ArrayList<Group> allGroups = new ArrayList<Group>();
	public static ArrayList<Person> localPersons = new ArrayList<Person>();
	public static ArrayList<Buddy> allBuddies = new ArrayList<Buddy>();
	
//	public static boolean hasBuddyInPhone(Context context) {
//		Database dbHelper = new Database(context);
//		dbHelper.open();
//		ArrayList<Buddy> buddies = dbHelper.fetchAllBuddies();
//		if (buddies == null || buddies.size() == 0) {
//			return false;
//		} else {
//			return true;
//		}
//	}

	/**
	 * fetch all persons for the field "allPersons"
	 * @param context
	 */
	public static void fFetchNormalPersons(Context context) {
		Database dbHelper = new Database(context);
//		allBuddies = dbHelper.fetchAllBuddies();
        allBuddies = dbHelper.fetchNormalBuddies();
		Log.i("buddy count in local database : " + allBuddies.size());
		changeBuddiesForPersons();
	}

	public static ArrayList<Buddy> fFetchPublicAccounts(Context context) {
		Database dbHelper = new Database(context);
		return dbHelper.fetchPublicAccounts();
	}

	public static ArrayList<Person> fFetchPublicAccountsAsPerson(Context context) {
		Database dbHelper = new Database(context);
		ArrayList<Buddy> buddies = dbHelper.fetchPublicAccounts();
		ArrayList<Person> result = new ArrayList<Person>(buddies.size());
		for (Buddy buddy : buddies) {
			result.add(Person.fromBuddy(buddy));
		}
		return result;
	}

	private static void changeBuddiesForPersons() {
		allPersons = new ArrayList<Person>();
		Person person;
		for (Buddy buddy : allBuddies) {
			person = Person.fromBuddy(buddy);
			allPersons.add(person);
		}
	}

	public static ArrayList<Group> fFetchAllGroups() {
		ArrayList<Group> groups = new ArrayList<Group>();
		if (allGroups.size() > 0)
			return groups;
		for (int i = 0; i < 4; i++) {
			Group group = new Group();
			group.setName(groupName[i]);
			allGroups.add(group);
		}
		return groups;
	}
	
	/**
	 * 获取本地联系人信息
	 * 
	 * @param context
	 * @return
	 */
	public static boolean _noContactInThisPhone = false;
	public static boolean _isGettingAllPerson = false;
	public static synchronized void fFetchAllLocalPerson(Context context) {
		_isGettingAllPerson = true;
		ArrayList<Person> persons;
//		Cursor personCursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
//		persons = fChangeLocalPersonToPerson(context, personCursor);
		persons = fFetchPhoneContacts(context);
//		if (personCursor != null && !personCursor.isClosed()) {
//			personCursor.close();
//			personCursor = null;
//		}
		localPersons = persons;
		if (localPersons == null || localPersons.size() == 0) {
			_noContactInThisPhone = true;
		} else {
			_noContactInThisPhone = false;
		}
		_isGettingAllPerson = false;
	}
	
	public static ArrayList<Person> fChangeLocalPersonToPerson(Context context, Cursor cursor) {
		ArrayList<Person> persons = new ArrayList<Person>();
		Person person = null;
		if (cursor != null) {
			while (cursor.moveToNext()) {
				person = new Person();
				
				//ID
				long contactId = 0;
				int idCol = cursor.getColumnIndex(ContactsContract.Contacts._ID);
				if (idCol >= 0) {
					contactId = cursor.getLong(idCol);
					person.setLocalContactID(contactId);
				}
				
				//显示名字
				idCol = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
				if (idCol >= 0) {
					String personName = cursor.getString(idCol);
					if (personName != null || !personName.equals("0")) {
						person.setName(personName);
					}
				}
				
				//照片ID
				idCol = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
				if (idCol >= 0) {
					long photoId = cursor.getLong(idCol);
					person.setLocalPersonPhotoID(photoId);
				}
				
				//SORTKEY
				idCol = cursor.getColumnIndex(ContactsContract.Contacts.SORT_KEY_PRIMARY);
				if (idCol >= 0) {
					String sortKey = cursor.getString(idCol);
					person.setSortKey(sortKey);
				}
				
				idCol = cursor.getColumnIndex(ContactsContract.Contacts.PHONETIC_NAME);
				if (idCol >= 0) {
					String phoneticName = cursor.getString(idCol);
					Log.e("phonetic name : " + phoneticName);
				}
				
				//电话信息
				idCol = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
				if (idCol >= 0) {
					String hasPhone = cursor.getString(idCol);
					if (hasPhone != null && hasPhone.compareTo("1") == 0) {
						Cursor phoneCur = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + String.valueOf(contactId), null, null);
						ArrayList<String[]> phoneList = new ArrayList<String[]>();
						String[] phone;
						while (phoneCur != null && phoneCur.moveToNext()) {
							phone = new String[3];
							String phoneNumber = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							if (phoneNumber == null) {
								phoneNumber = "";
							}
							phone[0] = phoneNumber;
							String phoneType = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
							if (phoneType == null) {
								phoneType = "";
							}
							phone[1] = phoneType;
							phone[2] = "-1";
							person.setGlobalPhoneNumber(phone[0]);
							phoneList.add(phone);
						}
						if (phoneCur != null && !phoneCur.isClosed()) {
							phoneCur.close();
						}
						phone = null;
						person.setPhones(phoneList);
					}
				}
				
				//邮件信息
				Cursor mailCur = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
						null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=" + String.valueOf(contactId), 
						null, null);
				if (mailCur != null && mailCur.moveToFirst()) {
					person.setEmailAddress(mailCur.getString(mailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)));
				}
				
				if (!cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))
						.startsWith("1730i")) {
					persons.add(person);
				}
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return persons;
	}

    /**
     * 获取本地电话号码信息
     * @param persons
     */
	private static final int DISPLAY_NAME_INDEX = 0;
	private static final int PHOTO_ID_INDEX = 1;
	private static final int CONTACT_ID_INDEX = 2;
	private static final int SORT_KEY_PRIMARY_INDEX = 3;
    private static final int _ID_INDEX = 4;
    private static final int NUMBER_INDEX = 5;
    private static final String[] PHONE_PROJECTION = new String[] {
            Phone.DISPLAY_NAME,
            Phone.PHOTO_ID,
            Phone.CONTACT_ID,
            Phone.SORT_KEY_PRIMARY,
            Phone._ID,
            Phone.NUMBER
    };
	public static synchronized ArrayList<Person> fFetchPhoneContacts(Context context) {
		ArrayList<Person> persons = new ArrayList<Person>();
		Person person = null;
//		final String[] PHONE_PROJECTION = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.PHOTO_ID, ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY};

		ContentResolver resolver = context.getContentResolver();
		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, PHONE_PROJECTION, null, null,
                ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
		if (phoneCursor != null && phoneCursor.getCount() != 0) {
			while(phoneCursor.moveToNext()) {
				person = new Person();
                String phoneNumber = phoneCursor.getString(NUMBER_INDEX);
                if (TextUtils.isEmpty(phoneNumber))
                    continue;
                person.setGlobalPhoneNumber(phoneNumber);
                String contactName = phoneCursor.getString(DISPLAY_NAME_INDEX);
                person.setName(contactName);
                long id = phoneCursor.getLong(_ID_INDEX);
                person.setLocalContactID(id);
                long contactId = phoneCursor.getLong(CONTACT_ID_INDEX);
                person.setLocalContactID(contactId);

//                String sortKey = phoneCursor.getString(SORT_KEY_PRIMARY_INDEX);
//                person.setSortKey(sortKey);
                String sortKey = Utils.makeSortKey(context, contactName);
                person.setSortKey(sortKey);
                long photoID = phoneCursor.getLong(PHOTO_ID_INDEX);
                person.setLocalPersonPhotoID(photoID);
                persons.add(person);
//				String contactName = phoneCursor.getString(DISPLAY_NAME_INDEX);
//				person.setName(contactName);
//
//				long contactId = phoneCursor.getLong(CONTACT_ID_INDEX);
//				person.setLocalContactID(contactId);
//
//				Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, new String[] {Data._ID, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL},
//						Data.CONTACT_ID + "=?" + " AND "
//								+ Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'",
//								new String[] {String.valueOf(contactId)}, null);
//				if (cursor != null && cursor.getCount() != 0) {
//					cursor.moveToFirst();
//					person.setGlobalPhoneNumber(cursor.getString(1));
//					if (cursor != null && !cursor.isClosed()) {
//						cursor.close();
//						cursor = null;
//					}
//				}
//
//				Cursor c = context.getContentResolver().query(Data.CONTENT_URI, new String[] {Data._ID, ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.LABEL},
//						Data.CONTACT_ID + "=?" + " AND "
//								+ Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'",
//								new String[] {String.valueOf(contactId)}, null);
//				if (c != null && c.getCount() != 0) {
//					c.moveToFirst();
//					person.setEmailAddress(c.getString(1));
//					if (c != null && !c.isClosed()) {
//						c.close();
//						c = null;
//					}
//				}
//
//				String sortKey = phoneCursor.getString(SORT_KEY_PRIMARY_INDEX);
//				person.setSortKey(sortKey);
//				Long photoId = phoneCursor.getLong(PHOTO_ID_INDEX);
//				person.setLocalPersonPhotoID(photoId);
//				persons.add(person);
			}
			if (phoneCursor != null && !phoneCursor.isClosed()) {
				phoneCursor.close();
			}
		}
		return persons;
	}

    private static long getPhotoIDByPhoneNumber(ContentResolver resolver, String phoneNumber) {
        Uri uri = Uri.parse("content://com.android.contacts/"
                                + "data/phones/filter/" + phoneNumber);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor.getLong(cursor.getColumnIndex("contact_id"));
        }
        return 0;
    }
	
	public static void initPersonSelect(ArrayList<Person> persons) {
		if (persons == null ||persons.size() == 0)
			return;
		for (Person person : persons) {
			person.setSelected(false);
		}
	}
	
	public static void fFetchPersonsFromBuddyByCondition(String condition, boolean ignoreCase) {
		curPersons = new ArrayList<Person>();
		char[] conditionChar = condition.toCharArray();
        if (ignoreCase) {
            conditionChar = condition.toLowerCase().toCharArray();
        }
		for (int i = 0; i < allPersons.size(); i++) {
			char[] nameChar = allPersons.get(i).getName().toCharArray();
            if (ignoreCase) {
                nameChar = allPersons.get(i).getName().toLowerCase().toCharArray();
            }
			if (isMatch(conditionChar, nameChar)) {
				curPersons.add(allPersons.get(i));
			}
		}
		curPersons.trimToSize();
	}
	
	public static void fFetchPersonsFromLocalByCondition(String condition, boolean ignoreCase) {
		curPersons = new ArrayList<Person>();
        char[] conditionChar = condition.toCharArray();
        if (ignoreCase) {
            conditionChar = condition.toLowerCase().toCharArray();
        }
		for (int i = 0; i < localPersons.size(); i++) {
//			char[] nameChar = localPersons.get(i).getName().toCharArray();
            char[] nameChar = localPersons.get(i).getSortKey().toCharArray();
            if (ignoreCase) {
//                nameChar = localPersons.get(i).getName().toLowerCase().toCharArray();
                nameChar = localPersons.get(i).getSortKey().toLowerCase().toCharArray();
            }
//            Log.i("test match with #"+condition+" for #"+localPersons.get(i).getSortKey());
			if (isMatch(conditionChar, nameChar)) {
				curPersons.add(localPersons.get(i));
			}
		}
		curPersons.trimToSize();
	}

//    private static boolean isMatch(char[] conditionChar, char[] nameChar) {
//        boolean isMatch=false;
//
//        for (int i = 0; i <conditionChar.length; i++) {
//            for (int j = 0; j < nameChar.length; j++) {
//                Log.i("compare "+conditionChar[i]+","+nameChar[j]);
//                if(conditionChar[i] == nameChar[j]) {
//                    isMatch=true;
//                    break;
//                }
//            }
//            if(isMatch) {
//                break;
//            }
//        }
//        return isMatch;
//    }
	
	private static boolean isMatch(char[] conditionChar, char[] nameChar) {
		int first = 0;
		boolean isMatch = false;
		for (int i = 0; i <conditionChar.length; i++) {
			for (int j = first; i < nameChar.length; j++) {
				if ((conditionChar.length - i) > (nameChar.length - j)) {
					return isMatch;
				} else {
					if (nameChar[j] == conditionChar[i]) {
						first = j + 1;
						break;
					}
				}
				first = j + 1;
			}
			if (first == nameChar.length && i < (conditionChar.length - 1)) {
				return isMatch;
			}
		}
		if (first == 0 || (conditionChar[conditionChar.length - 1] != nameChar[first - 1])) {
			isMatch = false;
		} else {
			isMatch = true;
		}
		return isMatch;
	}
	
	public static ArrayList<Object> fRecordArrayByPhoneNumber(Context context, String phoneNumber) {
		return new ArrayList<Object>();
	}
	
	/**
	 * 获取当前语言环境 en_US zh_CN ja_jp
	 * @return
	 */
	public static String getLocalLanguage() {
		Locale l = Locale.getDefault();
		return String.format("%s-%s", l.getLanguage(), l.getCountry());
	}
	
	public static String stripedPhoneNumber(String phoneNumber) {
		if (phoneNumber == null)
			return null;
		phoneNumber = phoneNumber.replace(" ", "");
		phoneNumber = phoneNumber.replace("-", "");
		phoneNumber = phoneNumber.replace("(", "");
		phoneNumber = phoneNumber.replace(")", "");
		
		if (phoneNumber.equals("")) {
			phoneNumber = null;
		}
		return phoneNumber;
	}

}
