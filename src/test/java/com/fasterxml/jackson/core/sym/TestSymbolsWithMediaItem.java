package com.fasterxml.jackson.core.sym;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.*;

public class TestSymbolsWithMediaItem extends com.fasterxml.jackson.core.BaseTest
{
    private final String JSON = aposToQuotes(
            "{'media' : {\n"
            +"      'uri' : 'http://foo.com',"
            +"      'title' : 'Test title 1',"
            +"      'width' : 640, 'height' : 480,"
            +"      'format' : 'video/mpeg4',"
            +"      'duration' : 18000000,"
            +"      'size' : 58982400,"
            +"      'bitrate' : 262144,"
            +"      'persons' : [ ],"
            +"      'player' : 'native',"
            +"      'copyright' : 'None'"
            +"   },\n"
            +"   'images' : [ {\n"
            +"      'uri' : 'http://bar.com',\n"
            +"      'title' : 'Test title 1',\n"
            +"      'width' : 1024,'height' : 768,\n"
            +"      'size' : 'LARGE'\n"
            +"    }, {\n"
            +"      'uri' : 'http://foobar.org',\n"
            +"      'title' : 'Javaone Keynote',\n"
            +"      'width' : 320, 'height' : 240,\n"
            +"      'size' : 'SMALL'\n"
            +"    } ]\n"
            +"}\n");

    public void testSmallSymbolSetWithBytes() throws IOException
    {
        final int SEED = 33333;

        ByteQuadsCanonicalizer symbolsRoot = ByteQuadsCanonicalizer.createRoot(SEED);
        ByteQuadsCanonicalizer symbols = symbolsRoot.makeChild(JsonFactory.Feature.collectDefaults());
        JsonFactory f = new JsonFactory();
        JsonParser p = f.createParser(JSON.getBytes("UTF-8"));

        JsonToken t;
        while ((t = p.nextToken()) != null) {
            if (t != JsonToken.FIELD_NAME) {
                continue;
            }
            String name = p.getCurrentName();
            int[] quads = calcQuads(name.getBytes("UTF-8"));

            if (symbols.findName(quads, quads.length) != null) {
                continue;
            }
            symbols.addName(name, quads, quads.length);
        }
        p.close();
        
        assertEquals(13, symbols.size());
        assertEquals(12, symbols.primaryCount()); // 80% primary hit rate
        assertEquals(1, symbols.secondaryCount()); // 13% secondary
        assertEquals(0, symbols.tertiaryCount()); // 7% tertiary
        assertEquals(0, symbols.spilloverCount()); // and couple of leftovers
    }

    public void testSmallSymbolSetWithChars() throws IOException
    {
        final int SEED = 33333;

        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(SEED);
        JsonFactory f = new JsonFactory();
        JsonParser p = f.createParser(JSON);

        JsonToken t;
        while ((t = p.nextToken()) != null) {
            if (t != JsonToken.FIELD_NAME) {
                continue;
            }
            String name = p.getCurrentName();
            char[] ch = name.toCharArray();
            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(name));
        }
        p.close();
        
        assertEquals(13, symbols.size());
        assertEquals(13, symbols.size());
        assertEquals(64, symbols.bucketCount());

        /* 30-Mar-2015, tatu: It is possible to tweak things to eliminate the
         *    last collision, but difficult to make it overall beneficial
         *    with other collision rate tests.
         */
        
        // collision count rather high, but has to do
        assertEquals(1, symbols.collisionCount());
        // as well as collision counts
        assertEquals(1, symbols.maxCollisionLength());
    }
}