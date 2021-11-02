package com.github.nicholasmoser.gnt4.seq.groups;

import com.github.nicholasmoser.gnt4.seq.EffectiveAddresses;
import com.github.nicholasmoser.gnt4.seq.opcodes.Add;
import com.github.nicholasmoser.gnt4.seq.opcodes.And;
import com.github.nicholasmoser.gnt4.seq.opcodes.Andws;
import com.github.nicholasmoser.gnt4.seq.opcodes.Decrement;
import com.github.nicholasmoser.gnt4.seq.opcodes.Divide;
import com.github.nicholasmoser.gnt4.seq.opcodes.Increment;
import com.github.nicholasmoser.gnt4.seq.opcodes.Movc;
import com.github.nicholasmoser.gnt4.seq.opcodes.Multiply;
import com.github.nicholasmoser.gnt4.seq.opcodes.Nimply;
import com.github.nicholasmoser.gnt4.seq.opcodes.Opcode;
import com.github.nicholasmoser.gnt4.seq.opcodes.Or;
import com.github.nicholasmoser.gnt4.seq.opcodes.Sub;
import com.github.nicholasmoser.gnt4.seq.opcodes.Subws;
import com.github.nicholasmoser.gnt4.seq.opcodes.UnknownOpcode;
import com.github.nicholasmoser.gnt4.seq.opcodes.Xor;
import com.github.nicholasmoser.utils.ByteStream;
import java.io.IOException;

public class OpcodeGroup04 {
  public static Opcode parse(ByteStream bs, byte opcodeByte) throws IOException {
    return switch (opcodeByte) {
      case 0x02 -> movc(bs);
      case 0x03 -> andws(bs);
      case 0x04 -> nimply(bs);
      case 0x05 -> inc(bs);
      case 0x06 -> dec(bs);
      case 0x07 -> add(bs);
      case 0x08 -> sub(bs);
      case 0x09 -> mul(bs);
      case 0x0A -> div(bs);
      case 0x0D -> and(bs);
      case 0x0E -> or(bs);
      case 0x0F -> xor(bs);
      case 0x11 -> subws(bs);
      case 0x15 -> op_0415(bs);
      case 0x17 -> op_0417(bs);
      default -> throw new IOException(String.format("Unimplemented: %02X", opcodeByte));
    };
  }

  private static Opcode movc(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Movc(offset, ea.getBytes(), info);
  }

  private static Opcode andws(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Andws(offset, ea.getBytes(), info);
  }

  private static Opcode nimply(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Nimply(offset, ea.getBytes(), info);
  }

  private static Opcode inc(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Increment(offset, ea.getBytes(), info);
  }

  private static Opcode dec(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Decrement(offset, ea.getBytes(), info);
  }

  private static Opcode add(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Add(offset, ea.getBytes(), info);
  }

  private static Opcode sub(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Sub(offset, ea.getBytes(), info);
  }

  private static Opcode mul(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Multiply(offset, ea.getBytes(), info);
  }

  private static Opcode div(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Divide(offset, ea.getBytes(), info);
  }

  private static Opcode and(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new And(offset, ea.getBytes(), info);
  }

  private static Opcode or(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Or(offset, ea.getBytes(), info);
  }

  private static Opcode xor(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Xor(offset, ea.getBytes(), info);
  }

  private static Opcode subws(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new Subws(offset, ea.getBytes(), info);
  }

  private static Opcode op_0415(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new UnknownOpcode(offset, ea.getBytes(), info);
  }

  private static Opcode op_0417(ByteStream bs) throws IOException {
    int offset = bs.offset();
    EffectiveAddresses ea = EffectiveAddresses.get(bs);
    String info = String.format(" %s", ea.getDescription());
    return new UnknownOpcode(offset, ea.getBytes(), info);
  }
}