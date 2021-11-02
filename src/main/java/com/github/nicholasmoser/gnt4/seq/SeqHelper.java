package com.github.nicholasmoser.gnt4.seq;

import com.github.nicholasmoser.gnt4.seq.opcodes.BinaryData;
import com.github.nicholasmoser.gnt4.seq.opcodes.BranchLink;
import com.github.nicholasmoser.gnt4.seq.opcodes.BranchLinkReturn;
import com.github.nicholasmoser.gnt4.seq.opcodes.Combo;
import com.github.nicholasmoser.gnt4.seq.opcodes.ComboList;
import com.github.nicholasmoser.gnt4.seq.opcodes.Opcode;
import com.github.nicholasmoser.gnt4.seq.opcodes.Pop;
import com.github.nicholasmoser.gnt4.seq.opcodes.Push;
import com.github.nicholasmoser.utils.ByteStream;
import com.github.nicholasmoser.utils.ByteUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SeqHelper {

  // Vanilla Combo translation for Oboro: コンボその
  private static final byte[] COMBO_JAPANESE = new byte[]{(byte) 0x83, 0x52, (byte) 0x83, (byte) 0x93, (byte) 0x83};
  // Vanilla Combo text for all other characters: 連打
  private static final byte[] REPEATED_HITS = new byte[]{(byte) 0x98, 0x41, (byte) 0x92, 0x65, (byte) 0x82};
  // Kosheh Combo translation for most characters
  private static final byte[] COMBO = "Combo".getBytes(StandardCharsets.UTF_8);
  // Kosheh Combo translation for Tayuyu doki demon
  private static final byte[] CHORD = "Chord".getBytes(StandardCharsets.UTF_8);
  // Kosheh Combo translation for Karasu
  private static final byte[] ROUTINE = "Routi".getBytes(StandardCharsets.UTF_8);

  /**
   * Returns the type of seq file this is.
   *
   * @param seqPath The path to the seq file.
   * @return The type of seq this file is.
   */
  public static SeqType getSeqType(Path seqPath) {
    String path = seqPath.toString();
    if (path.endsWith("0000.seq") && (path.contains("files/chr") || path.contains("files\\chr"))) {
      return SeqType.CHR_0000;
    }
    return SeqType.OTHER;
  }

  /**
   * Read bytes to see if the next group of bytes are binary data. If they are, return a list of
   * opcodes for them. Otherwise, return an empty list.
   *
   * @param bs The ByteStream to read from.
   * @param opcodes The list of current opcodes.
   * @param seqType The type of seq file this is.
   * @param uniqueBinaries The unique binaries encountered during parsing so far.
   * @return The list of binaries found, if any.
   * @throws IOException If an I/O error occurs.
   */
  public static List<Opcode> getBinaries(ByteStream bs, List<Opcode> opcodes, SeqType seqType,
      Set<String> uniqueBinaries) throws IOException {
    // There must be at least one opcode. Grab the last one for comparison.
    if (opcodes.isEmpty()) {
      return Collections.emptyList();
    }
    int offset = bs.offset();
    Opcode lastOpcode = opcodes.get(opcodes.size() - 1);

    if (seqType == SeqType.CHR_0000) {
      // These binaries will always be after a specific opcode
      if (lastOpcode instanceof ComboList) {
        // Make sure this is the last combo list
        if (SeqHelper.isComboList(bs)) {
          return Collections.singletonList(SeqHelper.readComboList(bs));
        }
        // There is binary data after the combo list that leads to the last set of opcodes
        return Collections.singletonList(SeqHelper.getBinaryUntilBranchAndLink(bs));
      }

      if (lastOpcode instanceof BranchLinkReturn) {
        if (SeqHelper.isChrLongBinary(opcodes)) {
          if (uniqueBinaries.contains("foundChrLongBinary")) {
            throw new IllegalStateException("There should only be one chr long binary.");
          }
          uniqueBinaries.add("foundChrLongBinary");
          return Collections.singletonList(SeqHelper.getChrLongBinary(bs));
        } else if (SeqHelper.isComboList(bs)) {
          return Collections.singletonList(SeqHelper.readComboList(bs));
        } else if (SeqHelper.isUnknownBinary3(bs)) {
          if (uniqueBinaries.contains("foundUnknownBinary3")) {
            throw new IllegalStateException("There should only be one unknown binary 3.");
          }
          uniqueBinaries.add("foundUnknownBinary3");
          return Collections.singletonList(SeqHelper.readUnknownBinary3(bs));
        } else if (SeqHelper.isUnknownBinary1(bs)) {
          if (uniqueBinaries.contains("foundUnknownBinary1")) {
            throw new IllegalStateException("There should only be one unknown binary 1.");
          }
          uniqueBinaries.add("foundUnknownBinary1");
          return Collections.singletonList(SeqHelper.readUnknownBinary1(bs));
        } else if (SeqHelper.isUnknownBinary4(bs)) {
          if (uniqueBinaries.contains("foundUnknownBinary4")) {
            throw new IllegalStateException("There should only be one unknown binary 4.");
          }
          uniqueBinaries.add("foundUnknownBinary4");
          return Collections.singletonList(SeqHelper.readUnknownBinary4(bs));
        } else if (SeqHelper.isUnknownBinary5(bs)) {
          if (uniqueBinaries.contains("foundUnknownBinary5")) {
            throw new IllegalStateException("There should only be one unknown binary 5.");
          }
          uniqueBinaries.add("foundUnknownBinary5");
          return Collections.singletonList(SeqHelper.readUnknownBinary5(bs));
        } else if (SeqHelper.isUnknownBinary6(bs)) {
          if (uniqueBinaries.contains("foundUnknownBinary6")) {
            throw new IllegalStateException("There should only be one unknown binary 6.");
          }
          uniqueBinaries.add("foundUnknownBinary6");
          return Collections.singletonList(SeqHelper.readUnknownBinary6(bs));
        }
      }

      // These binaries can be after multiple different opcodes
      if (SeqHelper.isOp04700Binary(bs)) {
        byte[] bytes = bs.readBytes(0x10);
        return Collections.singletonList(new BinaryData(offset, bytes, "; Binary data referenced by op_4700"));
      } else if (SeqHelper.isUnknownBinary2(bs)) {
        byte[] bytes = bs.readBytes(0x10);
        return Collections.singletonList(new BinaryData(offset, bytes));
      }
    }
    return Collections.emptyList();
  }

  /**
   * Reads binary data until a branch and link opcode and returns the binary data.
   *
   * @param bs The ByteStream to read from.
   * @return The binary data.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode getBinaryUntilBranchAndLink(ByteStream bs) throws IOException {
    int offset = bs.offset();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bs.peekWord() != 0x013C0000) {
      baos.write(bs.readBytes(4));
    }
    return new BinaryData(offset, baos.toByteArray());
  }

  /**
   * Reads a currently unknown 16-byte struct. The first three bytes are read at instructions
   * 0x8010704c, 0x80107050, and 0x80107054, which is used in opcodes such as op_4700.
   *
   * @param bs The ByteStream to read from.
   * @return If this is the binary for op_4700.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isOp04700Binary(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x10) {
      return false;
    }
    byte[] op_4700 = new byte[]{0x0, 0x0, 0x0, 0x4, 0x0, 0x0, 0x0, 0xA, 0x0, 0x0, 0x0, 0xA, 0x0,
        0x0, 0x0, 0x0};
    byte[] bytes = bs.peekBytes(0x10);
    return Arrays.equals(op_4700, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 1.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 1.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isUnknownBinary1(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x8) {
      return false;
    }
    byte[] expected = new byte[]{0x00, 0x20, 0x56, (byte) 0x80, 0x00, (byte) 0xF0, 0x00, 0x04};
    byte[] bytes = bs.peekBytes(0x8);
    return Arrays.equals(expected, bytes);
  }

  /**
   * Reads the binary data for unknown binary 1.
   *
   * @param bs The ByteStream to read from.
   * @return The binary data for unknown binary 1.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode readUnknownBinary1(ByteStream bs) throws IOException {
    int offset = bs.offset();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bs.peekWord() != 0x04026B00) {
      baos.write(bs.readBytes(4));
    }
    return new BinaryData(offset, baos.toByteArray());
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 2.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 2.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isUnknownBinary2(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x10) {
      return false;
    }
    byte[] expected = new byte[]{0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00,
        0x10, 0x00, 0x00, 0x00, 0x00};
    byte[] bytes = bs.peekBytes(0x10);
    return Arrays.equals(expected, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 3.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 3.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isUnknownBinary3(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x7B0) {
      return false;
    }
    byte[] expected = new byte[]{0x00, 0x5A, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02};
    byte[] bytes = bs.peekBytes(0x8);
    return Arrays.equals(expected, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 3.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 3.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode readUnknownBinary3(ByteStream bs) throws IOException {
    int offset = bs.offset();
    byte[] bytes = new byte[0x7B0];
    if (bs.read(bytes) != 0x7B0) {
      throw new IllegalStateException("Failed to read 0x7B0 bytes");
    } else if (bytes[0x7AD] != 0x7E) {
      throw new IllegalStateException("Third last byte should be 0x7E");
    }
    return new BinaryData(offset, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 4.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 4.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isUnknownBinary4(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x3C0) {
      return false;
    }
    byte[] expected = new byte[]{0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    byte[] bytes = bs.peekBytes(0x8);
    return Arrays.equals(expected, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 4.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 4.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode readUnknownBinary4(ByteStream bs) throws IOException {
    int offset = bs.offset();
    byte[] bytes = new byte[0x3C0];
    if (bs.read(bytes) != 0x3C0) {
      throw new IllegalStateException("Failed to read 0x3C0 bytes");
    } else if (bytes[0x3A8] != 0x43) {
      throw new IllegalStateException("Byte at size minus 0x18 should be 0x43");
    }
    return new BinaryData(offset, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 5.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 5.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isUnknownBinary5(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x1E4) {
      return false;
    }
    byte[] expected = new byte[]{0x00, 0x01, 0x00, 0x00, 0x63, 0x68, 0x72, 0x2F};
    byte[] bytes = bs.peekBytes(0x8);
    return Arrays.equals(expected, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 5.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 5.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode readUnknownBinary5(ByteStream bs) throws IOException {
    int offset = bs.offset();
    byte[] bytes = new byte[0x1E4];
    if (bs.read(bytes) != 0x1E4) {
      throw new IllegalStateException("Failed to read 0x1E4 bytes");
    } else if (bytes[0x1DF] != 0x66) {
      throw new IllegalStateException("Byte at size minus 0x5 should be 0x66");
    }
    return new BinaryData(offset, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 6.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 6.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isUnknownBinary6(ByteStream bs) throws IOException {
    if (bs.length() - bs.offset() < 0x2C) {
      return false;
    }
    byte[] expected = new byte[]{0x00, 0x00, 0x0B, 0x0D, 0x00, 0x00, 0x0B, 0x0F};
    byte[] bytes = bs.peekBytes(0x8);
    return Arrays.equals(expected, bytes);
  }

  /**
   * Returns if the ByteStream is currently at unknown binary 6.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is at unknown binary 6.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode readUnknownBinary6(ByteStream bs) throws IOException {
    int offset = bs.offset();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bs.peekWord() != 0x0908023F) {
      baos.write(bs.readBytes(4));
    }
    byte[] bytes = baos.toByteArray();
    int len = bytes.length;
    if (len != 0x2C) {
      throw new IllegalStateException("Unknown binary 6 must be of size 0x2C but is size " + len);
    }
    return new BinaryData(offset, bytes);
  }

  /**
   * The chr long binary is the longest binary data block in a chr file. It will come after a
   * function that has the following opcodes in the following order: push, push, bl, pop, pop, blr.
   *
   * @param opcodes The list of opcodes parsed so far.
   * @return If the next data is the unknown long binary in a chr 0000.seq file.
   */
  public static boolean isChrLongBinary(List<Opcode> opcodes) {
    if (opcodes.size() < 6) {
      return false;
    }
    int startPos = opcodes.size() - 1;
    return opcodes.get(startPos--) instanceof BranchLinkReturn
        && opcodes.get(startPos--) instanceof Pop
        && opcodes.get(startPos--) instanceof Pop
        && opcodes.get(startPos--) instanceof BranchLink
        && opcodes.get(startPos--) instanceof Push
        && opcodes.get(startPos) instanceof Push;
  }

  /**
   * Reads the the longest chr binary data block in a chr file. It is terminated with a movc
   * (0x04021366).
   *
   * @param bs The ByteStream to read from.
   * @return The binary data for the long chr 0000.seq data.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode getChrLongBinary(ByteStream bs) throws IOException {
    int offset = bs.offset();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bs.peekWord() != 0x04021366) {
      baos.write(bs.readBytes(4));
    }
    return new BinaryData(offset, baos.toByteArray());
  }

  /**
   * Return if the ByteStream is currently at a combo list.
   *
   * @param bs The ByteStream to read from.
   * @return If the ByteStream is currently at a combo list.
   * @throws IOException If an I/O error occurs.
   */
  public static boolean isComboList(ByteStream bs) throws IOException {
    // Skip null bytes until at non-null bytes
    bs.mark();
    int word;
    do {
      if (bs.offset() >= bs.length()) {
        bs.reset();
        return false; // EOF
      }
      word = bs.readWord();
    } while (word == 0);
    // Read the next 5 bytes
    if (bs.offset() + 5 > bs.length()) {
      bs.reset();
      return false; // EOF
    }
    byte[] bytes = bs.readBytes(5);
    bs.reset();
    return isCombo(bytes);
  }

  /**
   * Returns whether or not the given bytes are a combo definition. Most characters use "Combo" for
   * combo definitions, but a few characters have different text. The bytes passed into this method
   * must be 5 bytes or it will never return true.
   *
   * @param bytes The 5 bytes to check.
   * @return If the 5 bytes are a combo definition.
   */
  public static boolean isCombo(byte[] bytes) {
    return Arrays.equals(bytes, COMBO) || Arrays.equals(bytes, CHORD) ||
        Arrays.equals(bytes, ROUTINE) || Arrays.equals(bytes, COMBO_JAPANESE) ||
        Arrays.equals(bytes, REPEATED_HITS);
  }

  /**
   * Read the combo list from the ByteStream and return a ComboList opcode.
   *
   * @param bs The ByteStream to read from.
   * @return The ComboList opcode.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode readComboList(ByteStream bs) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int word;
    do {
      word = bs.readWord();
      baos.write(ByteUtils.fromInt32(word));
    } while (word == 0);
    int numberOfCombos = word;
    List<Combo> combos = new ArrayList<>(numberOfCombos);
    byte[] end = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    byte[] bytes = bs.readBytes(4);
    int offset = bs.offset();
    boolean parsingName = true;
    int comboNum = 1;
    while (!Arrays.equals(end, bytes)) {
      baos.write(bytes);
      if (bytes[3] == 0) {
        if (parsingName) {
          parsingName = false;
        } else {
          String combo = getComboText(baos.toByteArray());
          String info = String.format("Combo %d %s ", comboNum++, combo);
          if (bs.peekWord() == -1) {
            baos.write(end);
          }
          combos.add(new Combo(offset, baos.toByteArray(), info));
          baos = new ByteArrayOutputStream();
          offset = bs.offset();
          parsingName = true;
        }
      }
      bytes = bs.readBytes(4);
    }
    return new ComboList(combos);
  }

  /**
   * Gets the combo text for the given bytes. It will usually start with zeroes, followed by bytes
   * that correlate to the text for "Combo", followed by one or more zeroes, followed by the combo
   * itself. The combo can be represented in ASCII for the most part.
   *
   * @param bytes The bytes to parse the combo text for.
   * @return The combo in ASCII text, for the most party.
   */
  private static String getComboText(byte[] bytes) {
    boolean firstZeroes = true;
    boolean name = false;
    boolean theRest = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (byte chr : bytes) {
      if (firstZeroes && chr != 0) {
        firstZeroes = false;
        name = true;
      } else if (name && chr == 0) {
        name = false;
        theRest = true;
      } else if (theRest && chr != 0) {
        baos.write(chr);
      }
    }
    return baos.toString(StandardCharsets.US_ASCII);
  }

  /**
   * Returns null bytes as binary data, where the null bytes are 0xCCCCCCCC. This is currently
   * only used in mods in Super Clash of Ninja 4 (SCON4).
   *
   * @param bs The ByteStream to read from.
   * @return The null bytes as an opcode.
   * @throws IOException If an I/O error occurs.
   */
  public static Opcode getNullBytes(ByteStream bs) throws IOException {
    int offset = bs.offset();
    int word = bs.peekWord();
    if (word != 0xCCCCCCCC) {
      throw new IllegalStateException("Null word is not 0xCCCCCCCC at offset " + offset);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bs.peekWord() == 0xCCCCCCCC) {
      baos.write(bs.readBytes(4));
    }
    return new BinaryData(offset, baos.toByteArray(), " null bytes");
  }
}