package com.printflow.files;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Offline deterministic recommendations; this class makes no network requests and is not an AI model. */
public final class LocalDesignAssistantProvider implements DesignAssistantProvider {
    @Override public List<Suggestion> suggest(PrintDesign.Document source) {
        var document=source.asV2();var result=new ArrayList<Suggestion>();
        for(var page:document.pages()){
            for(var layer:page.elements())if("TEXT".equals(layer.type())){
                double centeredX=Math.max(0,(page.widthMm()-layer.widthMm())/2.0);
                if(Math.abs(layer.xMm()-centeredX)>10)result.add(new Suggestion(
                        "center-"+page.id()+"-"+layer.id(),"LOCAL SUGGESTION","Center this text block",
                        "The text block is noticeably off the page center. Preview and apply only if it suits the design.",
                        page.id(),layer.id(),"xMm",
                        Double.toString(layer.xMm()),Double.toString(centeredX)));
                double minX=page.safeMarginMm(),minY=page.safeMarginMm();
                double safeX=Math.max(minX,Math.min(layer.xMm(),page.widthMm()-minX-layer.widthMm()));
                double safeY=Math.max(minY,Math.min(layer.yMm(),page.heightMm()-minY-layer.heightMm()));
                if(Math.abs(layer.xMm()-safeX)>.5||Math.abs(layer.yMm()-safeY)>.5)result.add(new Suggestion(
                        "safe-"+page.id()+"-"+layer.id(),"LOCAL SUGGESTION","Move text into the safe area",
                        "This text block reaches beyond the configured safe margin. Review the page before applying.",
                        page.id(),layer.id(),"position",
                        layer.xMm()+","+layer.yMm(),safeX+","+safeY));
            }
        }
        return List.copyOf(result);
    }
}
