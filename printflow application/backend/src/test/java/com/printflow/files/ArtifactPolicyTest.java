package com.printflow.files;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class ArtifactPolicyTest {
 @Test void validMagicStillRequiresQuarantineAndScanning(){assertEquals(ArtifactPolicy.State.QUARANTINED,ArtifactPolicy.inspect("proof.pdf","application/pdf",100,"%PDF-1.7".getBytes()));assertFalse(ArtifactPolicy.downloadable(ArtifactPolicy.State.QUARANTINED));assertFalse(ArtifactPolicy.downloadable(ArtifactPolicy.State.REJECTED));}
 @Test void rejectsTraversalOversizeAndSpoofedOrExecutableTypes(){for(String name:new String[]{"../a.pdf","a/b.pdf","a\\b.pdf","payload.svg","archive.zip","proof.pdf.exe"})assertThrows(IllegalArgumentException.class,()->ArtifactPolicy.inspect(name,"application/pdf",100,"%PDF-".getBytes()));assertThrows(IllegalArgumentException.class,()->ArtifactPolicy.inspect("proof.pdf","application/pdf",ArtifactPolicy.MAX_BYTES+1,"%PDF-".getBytes()));assertThrows(IllegalArgumentException.class,()->ArtifactPolicy.inspect("proof.pdf","application/pdf",100,"<html>".getBytes()));}
}
