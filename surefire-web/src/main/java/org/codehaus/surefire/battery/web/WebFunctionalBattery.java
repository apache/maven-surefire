package org.codehaus.surefire.battery.web;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import HTTPClient.Cookie;
import HTTPClient.CookieModule;
import HTTPClient.CookiePolicyHandler;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.RoRequest;
import HTTPClient.RoResponse;
import org.codehaus.surefire.battery.AbstractBattery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WebFunctionalBattery extends AbstractBattery
    implements CookiePolicyHandler
{
    private String host;

    private int port;

    private HashMap connections;

    protected HTTPResponse resp;

    protected boolean acceptCookies;

    protected String data;

    private static String urlSearch;

    private static String urlReplace;

    public static void setURLReplace(String search, String replace)
    {
        urlSearch = search;
        urlReplace = replace;
    }

    public static String replaceURL(String url)
    {
        if(urlSearch == null || urlReplace == null)
            return url;
        int pos = url.indexOf(urlSearch);
        if(pos == -1)
        {
            return url;
        } else
        {
            String str = url.substring(0, pos) + urlReplace + url.substring(pos + urlSearch.length());
            return str;
        }
    }

    public WebFunctionalBattery()
    {
        host = "127.0.0.1";
        port = 80;
        connections = new HashMap();
        acceptCookies = true;
        try
        {
            HTTPConnection.removeDefaultModule(Class.forName("HTTPClient.RedirectionModule"));
        }
        catch(ClassNotFoundException cnfe) { }
        CookieModule.setCookiePolicyHandler(this);
    }

    public HTTPResponse getResponse()
    {
        return resp;
    }

    public void get(String url)
        throws Exception
    {
        get(url, null);
    }

    public void get(String url, List args)
        throws Exception
    {
        url = replaceURL(url);
        URL u = new URL(url);
        HTTPConnection conn = getConnection(u);
        if(args == null)
            resp = conn.Get(u.getFile());
        else
            resp = conn.Get(u.getFile(), listToNV(args));
    }

    public void post(String url)
        throws Exception
    {
        post(url, null);
    }

    public void post(String url, List args)
        throws Exception
    {
        url = replaceURL(url);
        URL u = new URL(url);
        HTTPConnection conn = getConnection(u);
        if(args == null)
            resp = conn.Post(u.getFile());
        else
            resp = conn.Post(u.getFile(), listToNV(args));
    }

    public void postMultiPart(String url, String data, int contLen)
        throws Exception
    {
        postMultiPart(url, data, contLen, null);
    }

    public void postMultiPart(String url, String data, int contLen, List args)
        throws Exception
    {
        url = replaceURL(url);
        URL u = new URL(url);
        HTTPConnection conn = getConnection(u);
        String boundary = data.substring(2, data.indexOf("\r"));
        List headers = new ArrayList(2);
        headers.add(new NVPair("Content-Type", " multipart/form-data; boundary=" + boundary));
        headers.add(new NVPair("Content-Length", "" + contLen));
        StringBuffer sb = null;
        if(args != null && args.size() > 0)
        {
            boolean firstpair = true;
            sb = new StringBuffer();
            for(Iterator iter = args.iterator(); iter.hasNext();)
            {
                NVPair nvpair = (NVPair)iter.next();
                if(firstpair)
                    sb.append("?");
                else
                    sb.append("&");
                sb.append(nvpair.getName() + "=" + nvpair.getValue());
                firstpair = false;
            }

        }
        StringBuffer fileSB = new StringBuffer(u.getFile());
        if(sb != null)
            fileSB.append(sb.toString());
        resp = conn.Post(fileSB.toString(), data, listToNV(headers));
    }

    public HTTPConnection getConnection(String url)
        throws Exception
    {
        return getConnection(new URL(url));
    }

    private HTTPConnection getConnection(URL url)
        throws Exception
    {
        String key = url.getHost() + ":" + url.getPort();
        HTTPConnection conn = (HTTPConnection)connections.get(key);
        if(conn == null)
        {
            conn = new HTTPConnection(url);
            conn.setAllowUserInteraction(false);
            connections.put(key, conn);
        }
        return conn;
    }

    public NVPair[] listToNV(List list)
    {
        return (NVPair[])list.toArray(new NVPair[0]);
    }

    public String urlDecode(String s)
    {
        return staticUrlDecode(s);
    }

    public static String staticUrlDecode(String s)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
        for(int count = 0; count < s.length(); count++)
            if(s.charAt(count) == '%')
            {
                count++;
                int a = Character.digit(s.charAt(count++), 16);
                a <<= 4;
                int b = Character.digit(s.charAt(count), 16);
                if(a + b == 39 || a + b == 132)
                    out.write(92);
                out.write(a + b);
            } else
            if(s.charAt(count) == '+')
                out.write(32);
            else
                out.write(s.charAt(count));

        return out.toString();
    }

    public boolean acceptCookie(Cookie cookie, RoRequest req, RoResponse resp)
    {
        return acceptCookies;
    }

    public boolean sendCookie(Cookie cookie, RoRequest req)
    {
        return acceptCookies;
    }

    protected void responseOK()
        throws ModuleException, IOException
    {
        int status = resp.getStatusCode();
        verify(status == 200 || status == 302 || status == 304, "Invalid HTTP response: " + resp + " for URI: " + resp.getEffectiveURI());
    }

    protected boolean responseContainsURI(String uri)
        throws ModuleException, IOException
    {
        return resp != null && resp.getEffectiveURI().getPath().indexOf(uri) != -1;
    }

    protected boolean responseContains(String text)
        throws ModuleException, IOException
    {
        if(resp == null || resp.getData() == null)
        {
            return false;
        } else
        {
            data = new String(resp.getData());
            return data.indexOf(text) != -1;
        }
    }

    protected void printResponse()
        throws Exception
    {
        System.err.println(new String(resp.getData()));
    }


}
