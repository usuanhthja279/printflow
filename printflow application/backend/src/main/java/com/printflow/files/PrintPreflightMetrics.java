package com.printflow.files;

/** Physical-size calculations shared by preflight rules and focused tests. */
public final class PrintPreflightMetrics {
    private PrintPreflightMetrics() {}
    public static double effectiveDpi(int sourceWidthPx,int sourceHeightPx,double widthMm,double heightMm) {
        if(sourceWidthPx<1||sourceHeightPx<1||widthMm<=0||heightMm<=0)throw new IllegalArgumentException("Source pixels and physical dimensions must be positive");
        double dpiX=sourceWidthPx/(widthMm/25.4),dpiY=sourceHeightPx/(heightMm/25.4);
        return Math.min(dpiX,dpiY);
    }
}
