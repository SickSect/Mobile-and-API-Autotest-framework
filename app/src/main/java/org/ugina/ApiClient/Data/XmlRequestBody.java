package org.ugina.ApiClient.Data;

public class XmlRequestBody implements IRequestBody {
    private final String xml;

    public XmlRequestBody(String xml) {
        this.xml = xml;
    }

    @Override
    public String content() {
        return xml;
    }

    @Override
    public String contentType() {
        return "application/xml";
    }
}
