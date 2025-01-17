package com.github.nicholasmoser.gnt4.seq.groups;

import com.github.nicholasmoser.gnt4.seq.SEQ_RegCMD1;
import com.github.nicholasmoser.gnt4.seq.SEQ_RegCMD2;
import com.github.nicholasmoser.gnt4.seq.opcodes.Opcode;
import com.github.nicholasmoser.gnt4.seq.opcodes.UnknownOpcode;
import com.github.nicholasmoser.utils.ByteStream;
import com.google.common.primitives.Bytes;
import java.io.IOException;

public class OpcodeGroup40 {

  public static Opcode parse(ByteStream bs, byte opcodeByte) throws IOException {
    return switch (opcodeByte) {
      case 0x01 -> UnknownOpcode.of(0x1C, bs);
      case 0x02 -> UnknownOpcode.of(0x20, bs);
      case 0x03 -> op_4003(bs);
      case 0x0B -> UnknownOpcode.of(0xC, bs);
      case 0x0C -> UnknownOpcode.of(0xC, bs);
      case 0x0D -> op_400D(bs);
      case 0x06 -> UnknownOpcode.of(0x8, bs);
      case 0x07 -> UnknownOpcode.of(0x8, bs);
      case 0x0E -> UnknownOpcode.of(0x4, bs);
      case 0x12 -> UnknownOpcode.of(0x10, bs);
      case 0x1E -> op_401E(bs);
      case 0x1F -> op_401F(bs);
      case 0x20 -> op_4020(bs);
      case 0x24 -> UnknownOpcode.of(0x4, bs);
      default -> throw new IOException(String.format("Unimplemented: %02X", opcodeByte));
    };
  }

  private static Opcode op_4003(ByteStream bs) throws IOException {
    int offset = bs.offset();
    SEQ_RegCMD1 ea = SEQ_RegCMD1.get(bs);
    byte[] bytes = bs.readBytes(0x20);
    return new UnknownOpcode(offset, Bytes.concat(ea.getBytes(), bytes), ea.getDescription());
  }

  private static Opcode op_400D(ByteStream bs) throws IOException {
    int offset = bs.offset();
    SEQ_RegCMD2 ea = SEQ_RegCMD2.get(bs);
    return new UnknownOpcode(offset, ea.getBytes(), ea.getDescription());
  }

  private static Opcode op_401E(ByteStream bs) throws IOException {
    int offset = bs.offset();
    SEQ_RegCMD2 ea = SEQ_RegCMD2.get(bs);
    return new UnknownOpcode(offset, ea.getBytes(), ea.getDescription());
  }

  private static Opcode op_401F(ByteStream bs) throws IOException {
    int offset = bs.offset();
    SEQ_RegCMD2 ea = SEQ_RegCMD2.get(bs);
    return new UnknownOpcode(offset, ea.getBytes(), ea.getDescription());
  }

  private static Opcode op_4020(ByteStream bs) throws IOException {
    int offset = bs.offset();
    SEQ_RegCMD2 ea = SEQ_RegCMD2.get(bs);
    return new UnknownOpcode(offset, ea.getBytes(), ea.getDescription());
  }
}
