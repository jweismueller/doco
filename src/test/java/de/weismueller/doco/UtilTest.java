package de.weismueller.doco;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

public class UtilTest {

    @Test
    public void test() throws Exception {
        String s = "";
        String[] split = s.split(",");
        Assert.isTrue(split.length == 1, "split.length=" + split.length);
    }

}
