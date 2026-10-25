package com.printflow.files;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class DesignPreflightServiceTest {
    @Test void reportsPageGeometryEmptyContentAndNoBleedWithoutClaimingPressReadiness() {
        var page=new PrintDesign.Page("page-1","Cover",90,54,0,5,List.of());
        var report=DesignPreflightService.inspect(PrintDesign.Document.v2("CARD",List.of(page),page.id()));
        assertTrue(report.stream().anyMatch(f->f.code().equals("PAGE_GEOMETRY")&&f.status().equals("PASS")));
        assertTrue(report.stream().anyMatch(f->f.code().equals("BLEED")&&f.status().equals("WARNING")));
        assertTrue(report.stream().anyMatch(f->f.code().equals("PAGE_CONTENT")&&f.status().equals("WARNING")));
        assertTrue(report.stream().anyMatch(f->f.code().equals("COLOR_MANAGEMENT")&&f.status().equals("NOT_CHECKED")));
        assertTrue(report.stream().anyMatch(f->f.code().equals("PDF_X_TRAPPING_SPOT_COLOR")&&f.status().equals("UNSUPPORTED")));
    }

    @Test void flagsObjectsOutsideBleedAndExplicitlyDisclosesIndicShapingLimitation() {
        var outside=new PrintDesign.Element("outside","RECTANGLE",80,45,20,15,0,null,12,"#ffffff","#183b43",0,null);
        var text=new PrintDesign.Element("indic","TEXT",2,2,40,20,0,"हिन्दी",12,"#183b43","#183b43",0,null);
        var page=new PrintDesign.Page("p","Page",90,54,3,5,List.of(outside,text));
        var report=DesignPreflightService.inspect(PrintDesign.Document.v2("CARD",List.of(page),"p"));
        assertTrue(report.stream().anyMatch(f->f.code().equals("OBJECT_OUTSIDE_BLEED")&&f.layerId().equals("outside")));
        assertTrue(report.stream().anyMatch(f->f.code().equals("COMPLEX_SCRIPT_SHAPING")&&f.status().equals("UNSUPPORTED")));
        assertTrue(report.stream().anyMatch(f->f.code().equals("TEXT_SAFE_MARGIN")&&f.layerId().equals("indic")));
    }
}
