package io.github.shamanalle.multiclicker.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileCodecTest {
    private static JsonObject sample() {
        return JsonParser.parseString("""
                {"version": 3, "modules": {"clicker": {"attack_interval": 7, "attack_mode": "HOLD"},
                 "auto_eat": {"enabled": true, "hunger": 12}}}""").getAsJsonObject();
    }

    @Test
    void roundTrip() {
        String text = ProfileCodec.encode("Iron farm", sample());
        assertTrue(text.startsWith(ProfileCodec.PREFIX));
        assertFalse(text.contains("\n") || text.contains(" "), "one line without spaces, easy to paste");
        ProfileCodec.Decoded decoded = ProfileCodec.decode(text);
        assertEquals("Iron farm", decoded.name());
        assertEquals(sample(), decoded.config());
    }

    @Test
    void spacesAroundThePastedTextAreIgnored() {
        String text = ProfileCodec.encode("A", sample());
        assertEquals(sample(), ProfileCodec.decode("  \n" + text + "\n ").config());
    }

    @Test
    void plainJsonIsAcceptedToo() {
        ProfileCodec.Decoded decoded = ProfileCodec.decode(sample().toString());
        assertEquals("", decoded.name());
        assertEquals(7, decoded.config().getAsJsonObject("modules").getAsJsonObject("clicker").get("attack_interval").getAsInt());
    }

    @Test
    void textThatIsNotAProfileIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(""));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode("hello"));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode("{\"name\": \"x\"}"));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode("{not json"));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(ProfileCodec.PREFIX + "%%%"));
        String text = ProfileCodec.encode("A", sample());
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(text.substring(0, text.length() / 2)));
    }

    @Test
    void hugeTextsAreRefused() {
        // A few kilobytes that inflate to megabytes must not be unpacked whole.
        byte[] spaces = " ".repeat(4 * ProfileCodec.MAX_SIZE).getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(spaces);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        String bomb = ProfileCodec.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(bomb));
    }
}
