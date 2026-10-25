package com.printflow.files;

/** Physical-size calculations shared by preflight rules and focused tests. */
public final class PrintPreflightMetrics {
    private PrintPreflightMetrics() {}
    public static double effectiveDpi(int sourceWidthPx,int sourceHeightPx,double widthMm,double heightMm) {
        return effectiveDpi(sourceWidthPx,sourceHeightPx,widthMm,heightMm,"STRETCH");
    }
    public static double effectiveDpi(int sourceWidthPx,int sourceHeightPx,double widthMm,double heightMm,String imageFit) {
        if(sourceWidthPx<1||sourceHeightPx<1||widthMm<=0||heightMm<=0)throw new IllegalArgumentException("Source pixels and physical dimensions must be positive");
        if(imageFit==null)imageFit="STRETCH";
        if(!java.util.Set.of("STRETCH","CONTAIN","COVER").contains(imageFit))throw new IllegalArgumentException("Unsupported image fitting mode");
        if(!"STRETCH".equals(imageFit)){
            double horizontalMmPerPixel=widthMm/sourceWidthPx,verticalMmPerPixel=heightMm/sourceHeightPx;
            double mmPerPixel="CONTAIN".equals(imageFit)?Math.min(horizontalMmPerPixel,verticalMmPerPixel):Math.max(horizontalMmPerPixel,verticalMmPerPixel);
            return 25.4/mmPerPixel;
        }
        double dpiX=sourceWidthPx/(widthMm/25.4),dpiY=sourceHeightPx/(heightMm/25.4);
        return Math.min(dpiX,dpiY);
    }
}
