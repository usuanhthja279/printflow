package com.printflow.files;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalDesignAssistantProviderTest {
    @Test void returnsDeterministicReviewableSuggestionsWithoutChangingTheDocument() {
        var layer=new PrintDesign.Element("title","TEXT",10,8,40,20,0,"Wedding day",20,"#183b43","#183b43",0,null);
        var page=new PrintDesign.Page("front","Front",90,54,3,5,List.of(layer));
        var document=PrintDesign.Document.v2("CARD",List.of(page),"front");
        var provider=new LocalDesignAssistantProvider();
        var first=provider.suggest(document);var second=provider.suggest(document);
        assertFalse(first.isEmpty());
        assertEquals(first,second);
        assertTrue(first.stream().allMatch(s->s.provider().equals("LOCAL SUGGESTION")));
        assertTrue(first.stream().allMatch(s->s.pageId().equals("front")&&s.layerId().equals("title")));
        assertEquals(10,document.pages().getFirst().elements().getFirst().xMm());
    }
}
