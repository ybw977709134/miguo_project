package co.onemeter.oneapp;

public class Constants {

    /**
     * biz 联系人/聊天记录 安全级别：
     *  1.normal：
     *  2.message protection:
     *  3.contact protection:
     *  4.high:
     */
    /**
     * biz 联系人/聊天记录 安全级别：
     *  1.normal：
     *  联系人和聊天纪录都保存在本地数据库。
     *  前后台切换时，不做任何操作。
     */
    public static final int SECURITY_LEVEL_NORMAL = 0;
    /**
     * biz 联系人/聊天记录 安全级别：
     *  2.message protection:
     *  不保存聊天纪录在本地数据库。
     *  切后台时，删除聊天信息；
     *  切前台时，不做任何操作。
     */
//    public static final int SECURITY_LEVEL_MESSAGE_PROTECTION = 1;
    /**
     * biz 联系人/聊天记录 安全级别：
     *  3.contact protection:
     *  不保存联系人在本地数据库。
     *  切后台时，删除群组/联系人；
     *  切前台时，自动下载群组/联系人。
     *  -----
     *  当点击某条聊天记录时，如果此记录对应的群组/联系人已存在于数据库中，直接显示；
     *  否则，先下载此群组/联系人。
     */
//    public static final int SECURITY_LEVEL_CONTACT_PROTECTION = 2;
    /**
     * biz 联系人/聊天记录 安全级别：
     *  4.high:
     *  聊天纪录，联系人，timeline都不保存在本地数据库。
     *  切后台时，删除群组/联系人，聊天记录，timeline。
     *  切前台时，自动下载群组/联系人/timeline。
     */
    public static final int SECURITY_LEVEL_HIGH = 3;
    
    //lessonId常量
    public static final String LESSONID = "lessonId";
    
    public final static String COMMA = ",";
    
    public final static String STUID = "STUID";
}
