package org.example.InputPoint.DICOM;

import org.dcm4che3.data.VR;
import org.example.util.DICOMUtil;

import java.util.HashMap;

public class TagDictionary {

    static class TagInfo {
        String tagName;
        VR vr;
        String xsd_datatype;

        public TagInfo(String tagName, VR vr) {
            this.tagName = tagName;
            this.vr = vr;
            this.xsd_datatype = DICOMUtil.DICOM2XSD(vr);
        }

    }

    private HashMap<String, TagInfo> tagDictionary = new HashMap<>();

    public void put(String tagCode, String tagName, VR vr) {
        if (!tagDictionary.containsKey(tagCode))
            tagDictionary.put(tagCode, new TagInfo(tagName, vr));
    }

    public TagInfo getTagInfo(String tagCode) {
        return tagDictionary.get(tagCode);
    }

    public String getTagName(String tagCode) {
        return tagDictionary.get(tagCode).tagName;
    }
    public VR getVr(String tagCode) {
        return tagDictionary.get(tagCode).vr;
    }
    public String getXsd_datatype(String tagCode) {
        return tagDictionary.get(tagCode).xsd_datatype;
    }


}
