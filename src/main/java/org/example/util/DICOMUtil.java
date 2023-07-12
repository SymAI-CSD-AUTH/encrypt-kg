package org.example.util;

import org.dcm4che3.data.VR;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DICOMUtil {

    public static String DICOM2XSD(VR vr) {
        if (vr == null)
            return "xsd:string";

        switch (vr) {

            case AE:    // Application Entity
            case AS:    // Age String
            case AT:    // Attribute Tag
            case CS:    // Code String
            case DS:    // Decimal String
            case IS:    // Integer String
            case LO:    // Long String
            case LT:    // Long Text
            case PN:    // Person Name
            case SH:    // Short String
            case ST:    // Short Text
            case UI:    // Unique Identifier

            case OB:    // Other Byte String
            case OW:    // Other Word String
            case OF:    // Other Float String
            case SQ:    // Sequence
            case UT:    // Unlimited Text
            case UN:    // Unknown
                return "xsd:string";

            case FL:    // Floating Point Single
                return "xsd:float";
            case FD:    // Floating Point Double
                return "xsd:double";

            case SL:    // Signed Long
                return "xsd:long";
            case UL:    // Unsigned Long
                return "xsd:unsignedLong";

            case SS:    // Signed Short
                return "xsd:short";
            case US:    // Unsigned Short
                return "xsd:unsignedShort";

            case DA:    // Date
                return "xs:date";
            case DT:    // Date Time
                return "xsd:dateTime";
            case TM:    // Time
                return "xs:time";
            default:
                return "xsd:string";
        }
    }

    public static String parseForTime(String value, VR vr) {
        try {
            if (vr == null)
                return value;
            switch (vr) {
                case DA:    // Date
                    return convertToXSDDate(value);
                case DT:    // Date Time
                    return convertToXSDDatetime(value);
                case TM:    // Time
                    return convertToHHMMSS(value);
                default:
                    return value;
            }
        }catch (Exception e) {
            return value;
        }
    }

    public static String convertToXSDDate(String date) {
        // Parse DICOM date string to LocalDate
        DateTimeFormatter dicomFormatter = DateTimeFormatter.ofPattern("uuuuMMdd");
        LocalDate localDate = LocalDate.parse(date, dicomFormatter);

        // Format LocalDate to XSD date string
        DateTimeFormatter xsdFormatter = DateTimeFormatter.ISO_DATE;
        String xsdDate = localDate.format(xsdFormatter);
        return xsdDate;
    }
    public static String convertToXSDDatetime(String datetime) {
        // Parse DICOM datetime string to LocalDateTime
        DateTimeFormatter dicomFormatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss.SSSSSS");
        LocalDateTime localDateTime = LocalDateTime.parse(datetime, dicomFormatter);

        // Format LocalDateTime to XSD datetime string
        DateTimeFormatter xsdFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String xsdDatetime = localDateTime.format(xsdFormatter);

        return xsdDatetime;
    }

    public static String convertToHHMMSS(String time) {
        // Remove trailing spaces
        time = time.trim();

        // Parse hours, minutes, and seconds
        int hours = Integer.parseInt(time.substring(0, 2));
        int minutes = Integer.parseInt(time.substring(2, 4));
        int seconds = Integer.parseInt(time.substring(4, 6));

        // Format as HH:MM:SS
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
