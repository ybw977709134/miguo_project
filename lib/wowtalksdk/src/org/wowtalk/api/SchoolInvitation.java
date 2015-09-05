package org.wowtalk.api;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by panzy on 8/21/15.
 */
public class SchoolInvitation {
    public int id;
    public String phone;
    public String schoolId;
    public String schoolName;
    /**
     * possible values:
     * null / "sent" / "accepted" / "rejected"
     */
    public String status;
    public String[] classroomIds;
    public String[] classroomNames;
    public Date lastModified;

    @Override
    public String toString() {
        return "SchoolInvitation{" +
                "schoolName='" + schoolName + '\'' +
                ", classroomNames=" + Arrays.toString(classroomNames) +
                '}';
    }
}
