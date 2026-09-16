import java.util.HexFormat;
import java.time.Instant;


public class GenesisHashCheck {
    public static void main(String[] args) {
        Block genesis = new Block(null, new byte[32], Instant.ofEpochMilli(1710000000000L));
        String hex = HexFormat.of().formatHex(genesis.computeBlockHash());
        System.out.println(hex);
        System.out.println("Matches expected: " + hex.equals("2f55162e9ac7e9e584677aeebbfc9f63aef043ad51a151cc633a3a1108f21488"));
    }
}