package org.wowtalk.api;

import android.content.Context;
import android.graphics.PointF;
import android.text.TextUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wowtalk.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 4/20/13
 * Time: 4:31 PM
 */
public class XmlHelper {

    public static WFile parseMultimedia(Element mediaNode) {
        WFile wf = new WFile();
        Element e;

        wf.remoteDbId = Utils.getFirstTextByTagName(mediaNode, "multimedia_content_id");

        e = Utils.getFirstElementByTagName(mediaNode, "multimedia_content_type");
        if(e != null && WFile.isValidExt(e.getTextContent())) {
            wf.setExt(e.getTextContent());
        } else {
            Log.e("bad media ext: " + (e == null ? "null" : e.getTextContent()) + ", xml: " + mediaNode.toString());
            return null;
        }

        wf.fileid = Utils.getFirstTextByTagName(mediaNode, "multimedia_content_path");
        wf.thumb_fileid = Utils.getFirstTextByTagName(mediaNode, "multimedia_thumbnail_path");
        wf.duration = Utils.getFirstIntByTagName(mediaNode, "duration", 0);
        return wf;
    }

    private static void parseOption(Element reviewNode, Moment.SurveyOption result) {
        Element e;
        e = Utils.getFirstElementByTagName(reviewNode, "option_id");
        if(e != null)
            result.optionId = e.getTextContent();
        e = Utils.getFirstElementByTagName(reviewNode, "description");
        if(e != null)
            result.optionDesc = e.getTextContent();
        e = Utils.getFirstElementByTagName(reviewNode, "vote_count");
        if(e != null)
            result.votedNum = Utils.tryParseInt(e.getTextContent(), 0);
        e = Utils.getFirstElementByTagName(reviewNode, "is_voted");
        if(e != null)
            result.isVoted = Utils.tryParseInt(e.getTextContent(), 0)==1;
    }

    public static void parseReview(Element reviewNode, Review result) {
        Element e;
        e = Utils.getFirstElementByTagName(reviewNode, "review_id");
        if(e != null)
            result.id = e.getTextContent();
        e = Utils.getFirstElementByTagName(reviewNode, "nickname");
        if(e != null)
            result.nickname = e.getTextContent();
        e = Utils.getFirstElementByTagName(reviewNode, "reviewer_uid");
        if(e != null)
            result.uid = e.getTextContent();
        e = Utils.getFirstElementByTagName(reviewNode, "comment");
        if(e != null)
            result.text = e.getTextContent();
        e = Utils.getFirstElementByTagName(reviewNode, "comment_type");
        if(e != null)
            result.type = Utils.tryParseInt(e.getTextContent(), Review.TYPE_UNKNOWN);

        result.replyToReviewId = Utils.getFirstTextByTagName(reviewNode, "reply_to_review_id");
        // 服务器端的review id 的数据类型是 int，0 表示无效值。
        // 客户端的 review id 的数据类型是 string，null/empty 表示无效值。
        if ("0".equals(result.replyToReviewId))
            result.replyToReviewId = "";

        e = Utils.getFirstElementByTagName(reviewNode, "reply_to_uid");
        if(e != null) {
            result.replyToUid = e.getTextContent();
        }
        e = Utils.getFirstElementByTagName(reviewNode, "reply_to_nickname");
        if(e != null) {
            result.replyToNickname = e.getTextContent();
        }
        e = Utils.getFirstElementByTagName(reviewNode, "insert_timestamp");
        if(e != null) {
            result.timestamp = Utils.tryParseLong(e.getTextContent(), 0);
        }

        result.hostId = Utils.getFirstTextByTagName(reviewNode, "moment_id");
        if (result.hostId == null)
            result.hostId = Utils.getFirstTextByTagName(reviewNode, "event_id");

        result.read = 1 == Utils.getFirstIntByTagName(reviewNode, "has_been_read", 1);
    }

    public static WEvent parseWEvent(Element eventNode) {
        Element e;
        WEvent a = new WEvent();
        e = Utils.getFirstElementByTagName(eventNode, "event_id");
        if(e != null)
            a.id = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "event_creator");
        if(e != null)
            a.catetoryName = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "owner_id");
        if(e != null)
            a.owner_uid = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "owner_name");
        if(e != null)
            a.host = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "text_title");
        if(e != null)
            a.title = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "text_content");
        if(e != null)
            a.description = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "event_type");
        if(e != null)
            a.event_type = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "joined_member");
        if(e != null)
            a.joinedMemberCount = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "max_member");
        if(e != null)
            a.capacity = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "possible_member");
        if(e != null)
            a.possibleJoinedMemberCount = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "contact_email");
        if(e != null) {
            a.contactEmail = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(eventNode, "area");
        if(e != null)
            a.address = e.getTextContent();

        e = Utils.getFirstElementByTagName(eventNode, "latitude");
        if(e != null)
            a.latitude = Utils.tryParseFloat(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "longitude");
        if(e != null)
            a.longitude = Utils.tryParseFloat(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "tag");
        if(e != null) {
            a.tag = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(eventNode, "category");
        if(e != null) {
            a.category = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(eventNode, "start_date");
        if(e != null) {
            a.event_start_date = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(eventNode, "start_timestamp");
        if(e != null) {
            a.startTime = new Date(1000 * Utils.tryParseLong(e.getTextContent(), 0));
            a.event_start_date = new SimpleDateFormat("yyyy-MM-dd").format(a.startTime);
        }

        e = Utils.getFirstElementByTagName(eventNode, "end_timestamp");
        if(e != null) {
            a.endTime = new Date(1000 * Utils.tryParseLong(e.getTextContent(), 0));
        }

        e = Utils.getFirstElementByTagName(eventNode, "deadline");
        if(e != null) {
            a.event_dead_line = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(eventNode, "is_joined");
        if(e != null)
            a.membership = Utils.tryParseInt(e.getTextContent(), WEvent.MEMBER_SHIP_NOT_JOIN);

        e = Utils.getFirstElementByTagName(eventNode, "timestamp");
        if(e != null) {
            a.timeStamp = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(eventNode, "thumbnail");
        if(e != null) {
            a.thumbNail = e.getTextContent();
        }


        //the following elements are kept unchanged as unused now
        e = Utils.getFirstElementByTagName(eventNode, "insert_timestamp");
        if(e != null)
            a.createdTime = new Date(1000 * Utils.tryParseLong(e.getTextContent(), 0));



        e = Utils.getFirstElementByTagName(eventNode, "member_count");
        if(e != null)
            a.size = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "is_official");
        if(e != null)
            a.isOfficial = "1".equals(e.getTextContent());



        e = Utils.getFirstElementByTagName(eventNode, "coin");
        if(e != null)
            a.costGolds = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "privacy_level");
        if(e != null)
            a.privacy_level = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "target_user_type");
        if(e != null)
            a.target_user_type = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(eventNode, "allow_review");
        if(e != null)
            a.allowReview = "1".equals(e.getTextContent());







        NodeList mediaList = eventNode.getElementsByTagName("multimedia");
        _parseMedias(mediaList, a);

        NodeList reviewNodes = eventNode.getElementsByTagName("review");
        _parseReviews(reviewNodes, a);

        return a;
    }

    public static Moment parseMoment(String defaultOwnerUid, Element momentNode,Context context,String oldMomentId) {
        Moment b = new Moment();

        // there is no owner field in the response,
        // since owner.userID is required when save moment into db,
        // we have to build a owner.
        b.owner = new Buddy();
        b.owner.userID = defaultOwnerUid;

        Element e = Utils.getFirstElementByTagName(
                momentNode, "moment_id");
        if(e != null)
            b.id = e.getTextContent();
        else
            return null;

        e = Utils.getFirstElementByTagName(momentNode, "uid");
        if(e != null)
            b.owner.userID = e.getTextContent();

        e = Utils.getFirstElementByTagName(momentNode, "nickname");
        if(e != null)
            b.owner.nickName = e.getTextContent();

        e = Utils.getFirstElementByTagName(
                momentNode, "latitude");
        if(e != null)
            b.latitude = Utils.tryParseFloat(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(
                momentNode, "longitude");
        if(e != null)
            b.longitude = Utils.tryParseFloat(e.getTextContent(), 0);

        b.place = Utils.getFirstTextByTagName(momentNode, "place");

        e = Utils.getFirstElementByTagName(
                momentNode, "text");
        if(e != null)
            b.text = e.getTextContent();

        e = Utils.getFirstElementByTagName(
                momentNode, "timestamp");
        if(e != null)
            b.timestamp = Utils.tryParseLong(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(momentNode, "privacy_level");
        if(e != null)
            b.privacyLevel = Utils.tryParseInt(e.getTextContent(), 0);

        e = Utils.getFirstElementByTagName(momentNode, "liked");
        if(e != null)
            b.likedByMe = "1".equals(e.getTextContent());

        e = Utils.getFirstElementByTagName(momentNode, "tag");
        if(e != null)
            b.tag = e.getTextContent();

        e = Utils.getFirstElementByTagName(momentNode, "deadline");
        if(e != null)
            b.surveyDeadLine = e.getTextContent();

        Database db = new Database(context);
        b.isFavorite=db.isMomentFavoriteLocal(TextUtils.isEmpty(oldMomentId)?b.id:oldMomentId);

        NodeList ml = momentNode.getElementsByTagName("multimedia");
        _parseMedias(ml, b);

        NodeList reviewNodes = momentNode.getElementsByTagName("review");
        _parseReviews(reviewNodes, b);

        NodeList optionNodes = momentNode.getElementsByTagName("option");
        _parseOptions(optionNodes, b);

        e = Utils.getFirstElementByTagName(momentNode, "sharerange");
        if(null != e) {
            NodeList shareRangeNodes = e.getElementsByTagName("group_id");
            _parseShareRange(shareRangeNodes, b);
        }
        return b;
    }

    public static void parseBuddy(Element buddyElement, Buddy result) {
        Element uidElement = Utils.getFirstElementByTagName(
                buddyElement, "uid");
        if(uidElement != null)
            result.userID = uidElement.getTextContent();

        Element nickElement = Utils.getFirstElementByTagName(
                buddyElement, "nickname");
        if(nickElement != null)
            result.nickName = nickElement.getTextContent();

        Element phoneEle = Utils.getFirstElementByTagName(buddyElement, "phone_number");
        if(phoneEle != null) {
            result.mobile = phoneEle.getTextContent();
        }

        Element last_status = Utils.getFirstElementByTagName(
                buddyElement, "last_status");
        if(last_status != null)
            result.status = last_status.getTextContent();

        Element timestamp = Utils.getFirstElementByTagName(
                buddyElement, "upload_photo_timestamp");
        if(timestamp != null && !Utils.isNullOrEmpty(timestamp.getTextContent()))
            result.photoUploadedTimeStamp = Utils.tryParseInt(timestamp.getTextContent(), -1);

        Element birthday = Utils.getFirstElementByTagName(
                buddyElement, "birthday");
        if (birthday != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            try {
                result.setBirthday(format.parse(birthday.getTextContent()));
            } catch (ParseException e) {
                result.setBirthday(new Date());
            }
        }

        Element sex = Utils.getFirstElementByTagName(
                buddyElement, "sex");
        if(sex != null && !Utils.isNullOrEmpty(sex.getTextContent())) {
            result.setSexFlag(Utils.tryParseInt(
                    sex.getTextContent(), Buddy.SEX_NULL));
        }

        Element area = Utils.getFirstElementByTagName(
                buddyElement, "area");
        if (area != null) {
            result.area = area.getTextContent();
        }

        Element pronunciation = Utils.getFirstElementByTagName(
                buddyElement, "pronunciation");
        if (pronunciation != null) {
            result.pronunciation = pronunciation.getTextContent();
        }

        Element interphone = Utils.getFirstElementByTagName(
                buddyElement, "interphone");
        if (interphone != null) {
            result.phoneNumber = interphone.getTextContent();
        }

        Element email = Utils.getFirstElementByTagName(
                buddyElement, "email_address");
        if (email != null) {
            result.setEmail(email.getTextContent());
        }

        Element jobTitle = Utils.getFirstElementByTagName(
                buddyElement, "title");
        if (jobTitle != null) {
            result.jobTitle = jobTitle.getTextContent();
        }

        Element employeeId = Utils.getFirstElementByTagName(
                buddyElement, "employee_id");
        if (employeeId != null) {
            result.employeeId = employeeId.getTextContent();
        }

        Element dev = Utils.getFirstElementByTagName(buddyElement, "device_number");
        if(dev != null) {
            result.deviceNumber = dev.getTextContent();
        }

        Element appver = Utils.getFirstElementByTagName(buddyElement, "app_ver");
        if(appver != null) {
            result.appVer = appver.getTextContent();
        }

        Element buddyflag = Utils.getFirstElementByTagName(buddyElement, "buddy_flag");
        if(buddyflag != null) {
            // nothing to do
        }

        Element e = Utils.getFirstElementByTagName(buddyElement, "relationship");
        if(e != null) {
            result.setFriendshipWithMe(
                    Utils.tryParseInt(e.getTextContent(), Buddy.RELATIONSHIP_NONE));
        }

        e = Utils.getFirstElementByTagName(buddyElement, "last_longitude");
        if(e != null && !Utils.isNullOrEmpty(e.getTextContent())) {
            if(result.lastLocation == null) {
                result.lastLocation = new WLocation(0, 0);
            }
            result.lastLocation.longitude = Utils.tryParseDouble(e.getTextContent(), 0);
        }

        e = Utils.getFirstElementByTagName(buddyElement, "last_latitude");
        if(e != null && !Utils.isNullOrEmpty(e.getTextContent())) {
            if(result.lastLocation == null) {
                result.lastLocation = new WLocation(0, 0);
            }
            result.lastLocation.latitude = Utils.tryParseDouble(e.getTextContent(), 0);
        }
        e = Utils.getFirstElementByTagName(buddyElement, "last_login_timestamp");
        if(e != null && !Utils.isNullOrEmpty(e.getTextContent())) {
            result.lastOnline = new Date(Utils.tryParseInt(e.getTextContent(), 0));
        }

        e = Utils.getFirstElementByTagName(buddyElement, "user_type");
        if(e != null && !Utils.isNullOrEmpty(e.getTextContent())) {
            result.setAccountType(Utils.tryParseInt(e.getTextContent(), Buddy.ACCOUNT_TYPE_NULL));
        }

        e = Utils.getFirstElementByTagName(buddyElement, "wowtalk_id");
        if(e != null) {
            result.wowtalkID = e.getTextContent();
        }

        e = Utils.getFirstElementByTagName(buddyElement, "block_msg");
        if(e != null) {
            result.willBlockMsg = "1".equals(e.getTextContent());
        }

        e = Utils.getFirstElementByTagName(buddyElement, "block_msg_notification");
        if(e != null) {
            result.willBlockNotification = "1".equals(e.getTextContent());
        }

        e = Utils.getFirstElementByTagName(buddyElement, "favorite");
        if(e != null) {
            result.favorite = "1".equals(e.getTextContent());
        }

        e = Utils.getFirstElementByTagName(buddyElement, "alias");
        if(e != null) {
            result.alias = e.getTextContent();
        }
    }

    public static void parseGroup(Element ge, GroupChatRoom result) {
        if (null == result)
            return;

        Element e;

        e = Utils.getFirstElementByTagName(ge, "group_id");
        if (null != e)
            result.groupID = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "short_group_id");
        if (null != e)
            result.shortGroupID = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "name");
        if (null != e)
            result.groupNameOriginal = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "status");
        if (null != e)
            result.groupStatus = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "intro");
        if (null != e)
            result.description = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "latitude");
        if (null != e) {
            if (null == result.location)
                result.location = new PointF();
            result.location.y = Utils.tryParseFloat(e.getTextContent(), 0);
        }
        e = Utils.getFirstElementByTagName(ge, "longitude");
        if (null != e) {
            if (null == result.location)
                result.location = new PointF();
            result.location.x = Utils.tryParseFloat(e.getTextContent(), 0);
        }
        e = Utils.getFirstElementByTagName(ge, "place");
        if (null != e)
            result.place = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "upload_photo_timestamp");
        if (null != e)
            result.setPhotoUploadedTimestamp(Utils.tryParseLong(e.getTextContent(), 0));
        e = Utils.getFirstElementByTagName(ge, "category");
        if (null != e)
            result.category = e.getTextContent();
        e = Utils.getFirstElementByTagName(ge, "temp_group_flag");
        if (null != e)
            result.isTemporaryGroup = "1".equals(e.getTextContent());
        e = Utils.getFirstElementByTagName(ge, "member_count");
        if (null != e)
            result.memberCount = Utils.tryParseInt(e.getTextContent(), 0);
        e = Utils.getFirstElementByTagName(ge, "max_member");
        if (null != e)
            result.maxNumber = Utils.tryParseInt(e.getTextContent(), 0);
        e = Utils.getFirstElementByTagName(ge, "is_group_name_changed");
        if (null != e) {
            result.isGroupNameChanged = Utils.tryParseInt(e.getTextContent(), 0) == 1;
        }
        e = Utils.getFirstElementByTagName(ge, "is_member");
        if (null != e) {
            result.isMeBelongs = Utils.tryParseInt(e.getTextContent(), 1) == 1;
        }else {
            result.isMeBelongs = true;
        }
        e = Utils.getFirstElementByTagName(ge, "editable");
        if (null != e) {
            result.isEditable = Utils.tryParseInt(e.getTextContent(), 1) == 1;
        } else {
            result.isEditable = true;
        }
        e = Utils.getFirstElementByTagName(ge, "parent_id");
        if (null != e) {
            result.parentGroupId = e.getTextContent();
        }
        e = Utils.getFirstElementByTagName(ge, "weight");
        if (null != e) {
            result.weight = Utils.tryParseInt(e.getTextContent(), 0);
        }

        result.willBlockMsg = 1 == Utils.getFirstIntByTagName(ge, "blog_msg", 0);
        result.willBlockNotification = 1 == Utils.getFirstIntByTagName(ge, "blog_msg_notification", 0);
        result.myNickHere = Utils.getFirstTextByTagName(ge, "nickname_in_group");
        result.groupNameLocal = Utils.getFirstTextByTagName(ge, "alias");
    }

    public static void parseGroupMember(Element buddyElement, GroupMember result) {
        parseBuddy(buddyElement, result);

        Element e = Utils.getFirstElementByTagName(buddyElement, "level");
        if(e != null && !Utils.isNullOrEmpty(e.getTextContent())) {
            result.setLevel(Utils.tryParseInt(e.getTextContent(), 0));
        }

        e = Utils.getFirstElementByTagName(buddyElement, "msg");
        if(e != null && !Utils.isNullOrEmpty(e.getTextContent())) {
            result.message = e.getTextContent();
        }
    }

    private static void _parseMedias(NodeList mediaNodes, IHasMultimedia a) {
        if (mediaNodes == null || a == null)
            return;

        if(mediaNodes != null && mediaNodes.getLength() > 0) {
            for(int j = 0, m = mediaNodes.getLength(); j < m; ++j) {
                Element mediaNode = (Element)mediaNodes.item(j);
                WFile wf = XmlHelper.parseMultimedia(mediaNode);
                if (wf != null)
                    a.addMedia(wf);
            }
        }
    }

    private static void _parseShareRange(NodeList shareRangeNodes, Moment moment) {
        if (shareRangeNodes == null || moment == null)
            return;

        if(shareRangeNodes != null && shareRangeNodes.getLength() > 0) {
            StringBuilder sb=new StringBuilder();
            for(int j = 0, m = shareRangeNodes.getLength(); j < m; ++j) {
                Element optionNode = (Element)shareRangeNodes.item(j);
                sb.append(optionNode.getTextContent());
                if(j != shareRangeNodes.getLength()-1) {
                    sb.append(Moment.LIMITED_DEPARTMENT_SEP);
                }
            }
            moment.shareRange=Moment.SERVER_SHARE_RANGE_LIMITED;
            moment.setLimitedDepartment(sb.toString());
        } else {
            moment.shareRange=Moment.SERVER_SHARE_RANGE_PUBLIC;
        }
    }

    private static void _parseOptions(NodeList optionNodes, Moment moment) {
        if (optionNodes == null || moment == null)
            return;

        if(Moment.SERER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(moment.tag)) {
            moment.isSurveyAllowMultiSelect=true;
        } else {
            moment.isSurveyAllowMultiSelect=false;
        }

        if(optionNodes != null && optionNodes.getLength() > 0) {
            for(int j = 0, m = optionNodes.getLength(); j < m; ++j) {
                Element optionNode = (Element)optionNodes.item(j);
                Moment.SurveyOption option = new Moment.SurveyOption();
                option.momentId=moment.id;
                XmlHelper.parseOption(optionNode, option);
                moment.surveyOptions.add(option);
                Log.w("parser option "+option.optionDesc+", j="+j+", m="+m);
            }
        }
    }

    private static void _parseReviews(NodeList reviewNodes, IHasReview a) {
        if (reviewNodes == null || a == null)
            return;

        if(reviewNodes != null && reviewNodes.getLength() > 0) {
            for(int j = 0, m = reviewNodes.getLength(); j < m; ++j) {
                Element reviewNode = (Element)reviewNodes.item(j);
                Review r = new Review();
                XmlHelper.parseReview(reviewNode, r);
                a.addReview(r);
            }
        }
    }

    public static void parseUpdatesInfo(Element infoElement, UpdatesInfo result) {
        if (null == infoElement || null == result)
            return;

        Element e;
        result.versionCode = Utils.getFirstIntByTagName(infoElement, "ver_code", 0);
        result.versionName = Utils.getFirstTextByTagName(infoElement, "ver_name");
        result.md5sum = Utils.getFirstTextByTagName(infoElement, "md5sum");
        result.link = Utils.getFirstTextByTagName(infoElement, "link");
        e = Utils.getFirstElementByTagName(infoElement, "change_log");
        if (null != e) {
            NodeList changelogs = e.getElementsByTagName("li");
            if (null != changelogs && 0 < changelogs.getLength()) {
                int n = changelogs.getLength();
                result.changeLog = new String[n];
                for(int j = 0; j < n; ++j) {
                    e = (Element)changelogs.item(j);
                    if (null != e) {
                        result.changeLog[j] = e.getTextContent();
                    }
                }
            }
        }
    }

    /**
     * parse favorite contacts and groups
     * @param favortiteElement
     * @param favorites 
     * @return results[0],item_id; results[1],type
     */
    public static void parseFavoriteContactsAndGrouops(Element favortiteElement, String[] favorites) {
        Element e;
        // 每次解析需要对favorites数组重新赋值，因为数组是重用的
        e = Utils.getFirstElementByTagName(favortiteElement, "item_id");
        favorites[0] = (null != e) ? e.getTextContent() : "";
        e = Utils.getFirstElementByTagName(favortiteElement, "type");
        favorites[1] = (null != e) ? e.getTextContent() : "";
    }

    /**
     * parse chat message
     * @param isGroupChat 
     * @param chatRecord xml Element
     * @param chatMessage ChatMessage
     * @param uid the uid of mine
     */
    public static void parseHistoryChatMessage(Boolean isGroupChat, Element chatRecord, ChatMessage chatMessage, String uid) {
        Element element = null;

        if (isGroupChat) {
            chatMessage.isGroupChatMessage = true;
            element = Utils.getFirstElementByTagName(chatRecord, "groupchat_sender");
            if(element != null) {
                String groupSender = element.getTextContent();
                chatMessage.groupChatSenderID = groupSender;
                if (uid.equals(groupSender)) {
                    chatMessage.ioType = ChatMessage.IOTYPE_OUTPUT;
                } else {
                    chatMessage.ioType = ChatMessage.IOTYPE_INPUT_READED;
                }
            }

            // groupchat_sender是自己时（即ioType == IOTYPE_OUTPUT），群组名包含在"to_uid"节点
            // 否则，群组名包含在"from_uid"节点
            if (chatMessage.ioType == ChatMessage.IOTYPE_OUTPUT) {
                element = Utils.getFirstElementByTagName(chatRecord, "to_uid");
            } else {
                element = Utils.getFirstElementByTagName(chatRecord, "from_uid");
            }
            if(element != null) {
                chatMessage.chatUserName = element.getTextContent();
            }
        } else {
            chatMessage.isGroupChatMessage = false;
            element = Utils.getFirstElementByTagName(chatRecord, "from_uid");
            if(element != null) {
                String from = element.getTextContent();
                if (uid.equals(from)) {
                    chatMessage.ioType = ChatMessage.IOTYPE_OUTPUT;
                } else {
                    chatMessage.ioType = ChatMessage.IOTYPE_INPUT_READED;
                    chatMessage.chatUserName = from;
                }
            }

            element = Utils.getFirstElementByTagName(chatRecord, "to_uid");
            if(element != null) {
                String to = element.getTextContent();
                if (!uid.equals(to)) {
                    chatMessage.chatUserName = to;
                }
            }
        }

        element = Utils.getFirstElementByTagName(chatRecord, "sentdate");
        if(element != null) {
            chatMessage.sentDate = element.getTextContent();
        }

        element = Utils.getFirstElementByTagName(chatRecord, "type");
        if(element != null) {
            chatMessage.msgType = element.getTextContent();
        }

        element = Utils.getFirstElementByTagName(chatRecord, "message");
        if(element != null) {
            chatMessage.messageContent = element.getTextContent();
        }
    }

    public static void parseOfflineMessage(Element chatRecord, ChatMessage chatMessage, String uid) {
        Element element = null;
        // 1. 先判断是否是群组会话
        element = Utils.getFirstElementByTagName(chatRecord, "groupchat_sender");
        if(element != null) {
            String groupSender = element.getTextContent();
            if (TextUtils.isEmpty(groupSender)) {
                chatMessage.isGroupChatMessage = false;
            } else {
                chatMessage.isGroupChatMessage = true;
                chatMessage.groupChatSenderID = groupSender;
            }
        }

        // 2. 根据是否是群组会话，计算出 ioType,chatUserName
        if (chatMessage.isGroupChatMessage) {
            // groupchat_sender是自己时，群组名包含在"to_uid"节点
            if (chatMessage.groupChatSenderID.equals(uid)) {
                chatMessage.ioType = ChatMessage.IOTYPE_OUTPUT;
                element = Utils.getFirstElementByTagName(chatRecord, "to_uid");
            } else {
                // 否则，群组名包含在"from_uid"节点
                chatMessage.ioType = ChatMessage.IOTYPE_INPUT_UNREAD;
                element = Utils.getFirstElementByTagName(chatRecord, "from_uid");
            }
            if(element != null) {
                chatMessage.chatUserName = element.getTextContent();
            }
        } else {
            element = Utils.getFirstElementByTagName(chatRecord, "from_uid");
            if(element != null) {
                String from = element.getTextContent();
                if (uid.equals(from)) {
                    chatMessage.ioType = ChatMessage.IOTYPE_OUTPUT;
                } else {
                    chatMessage.ioType = ChatMessage.IOTYPE_INPUT_UNREAD;
                    chatMessage.chatUserName = from;
                }
            }

            element = Utils.getFirstElementByTagName(chatRecord, "to_uid");
            if(element != null) {
                String to = element.getTextContent();
                if (!uid.equals(to)) {
                    chatMessage.chatUserName = to;
                }
            }
        }
        // 发出的消息，状态都使用SENTSTATUS_SENT
        if (ChatMessage.IOTYPE_OUTPUT.equals(chatMessage.ioType)) {
            chatMessage.sentStatus = ChatMessage.SENTSTATUS_SENT;
        }

        // 3. 其他属性
        element = Utils.getFirstElementByTagName(chatRecord, "sentdate");
        if(element != null) {
            chatMessage.sentDate = element.getTextContent();
            chatMessage.uniqueKey = Database.chatMessageSentDateToUniqueKey(chatMessage.sentDate);
        }

        element = Utils.getFirstElementByTagName(chatRecord, "type");
        if(element != null) {
            chatMessage.msgType = element.getTextContent();
        }

        element = Utils.getFirstElementByTagName(chatRecord, "message");
        if(element != null) {
            chatMessage.messageContent = element.getTextContent();
        }
    }

    public static void parseLatestChatTarget(Element chatRecord, LatestChatTarget chatTarget, String uid) {
        Element element = null;
        String fromUid = "";
        element = Utils.getFirstElementByTagName(chatRecord, "from_uid");
        if(element != null) {
            fromUid = element.getTextContent();
        }
        String toUid = "";
        element = Utils.getFirstElementByTagName(chatRecord, "to_uid");
        if(element != null) {
            toUid = element.getTextContent();
        }
        // from_uid,to_uid 中必定有一个是自己uid，则另一个就是target_id
        if (!TextUtils.isEmpty(fromUid) && fromUid.equals(uid)) {
            chatTarget.targetId = toUid;
        }
        if (!TextUtils.isEmpty(toUid) && toUid.equals(uid)) {
            chatTarget.targetId = fromUid;
        }

        element = Utils.getFirstElementByTagName(chatRecord, "groupchat_sender");
        if(element != null) {
            chatTarget.isGroup = !TextUtils.isEmpty(element.getTextContent());
        }

        element = Utils.getFirstElementByTagName(chatRecord, "sentdate");
        if(element != null) {
            chatTarget.sentDate = element.getTextContent();
        }
    }

    public static String parseRootDeptMembers(Context context, Element rootDeptElement,
            ArrayList<String> buddyIds, ArrayList<Buddy> buddies) {

        String myUid = PrefUtil.getInstance(context).getUid();
        Element groupIdElement = Utils.getFirstElementByTagName(rootDeptElement, "group_id");
        String rootDeptId = null;
        if (null != groupIdElement) {
            rootDeptId = groupIdElement.getTextContent();
        }

        NodeList buddyNodes = rootDeptElement.getElementsByTagName("member");
        for (int i = 0; i < buddyNodes.getLength(); i++) {
            Element buddyElement = (Element) buddyNodes.item(i);
            Element uidElement = Utils.getFirstElementByTagName(buddyElement, "uid");
            if (null != uidElement) {
                Buddy buddy = new Buddy();
                String uid = uidElement.getTextContent();
                if (!TextUtils.isEmpty(uid)) {
                    buddy.userID = uid;
                    buddyIds.add(uid);

                    Element nicknameElement = Utils.getFirstElementByTagName(buddyElement, "nickname");
                    if (null != nicknameElement) {
                        buddy.nickName = nicknameElement.getTextContent();
                    }
                    Element pronunciationElement = Utils.getFirstElementByTagName(buddyElement, "pronunciation");
                    if (null != pronunciationElement) {
                        buddy.pronunciation = pronunciationElement.getTextContent();
                    }
                    Element photoTimeStampElement = Utils.getFirstElementByTagName(buddyElement, "upload_photo_timestamp");
                    if (null != photoTimeStampElement) {
                        String photoTimeStamp = photoTimeStampElement.getTextContent();
                        if (!TextUtils.isEmpty(photoTimeStamp)) {
                            try {
                                buddy.photoUploadedTimeStamp = Long.parseLong(photoTimeStamp);
                            } catch (NumberFormatException exception) {
                            }
                        }
                    } else {
                        buddy.photoUploadedTimeStamp = -1;
                    }
                    Element lastStatusElement = Utils.getFirstElementByTagName(buddyElement, "last_status");
                    if (null != lastStatusElement) {
                        buddy.status = lastStatusElement.getTextContent();
                    }
                    buddies.add(buddy);
                }
            }
        }

        return rootDeptId;
    }


    public static String parseNormalDeptMemberIds(Element normalDeptElement, ArrayList<String> buddyIds) {
        String groupId = null;
        Element groupIdElement = Utils.getFirstElementByTagName(normalDeptElement, "group_id");
        if (null != groupIdElement) {
            groupId = groupIdElement.getTextContent();
        }
        NodeList buddyIdNodes = normalDeptElement.getElementsByTagName("member");
        for (int i = 0; i < buddyIdNodes.getLength(); i++) {
            Element buddyIdElement = (Element) buddyIdNodes.item(i);
            buddyIds.add(buddyIdElement.getTextContent());
        }
        return groupId;
    }

}
