package com.github.nicholasmoser.graphics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import com.github.nicholasmoser.utils.ByteStream;
import com.github.nicholasmoser.utils.ByteUtils;
import com.google.common.primitives.UnsignedInteger;

public class TXG {

    private static long[] Offsets;
    private static int FileCount;

    public static void pack(String output, String[] files) throws IOException, DataFormatException {
        RandomAccessFile outputFile = new RandomAccessFile(output, "rw");
        ByteUtils.writeInt32(outputFile, files.length);
        Offsets = new long[files.length];
        for (int i = 0; i < files.length; i++){
            ByteUtils.writeInt32(outputFile, 0);
        }
        ByteUtils.byteAlign(outputFile,0x20);

        for (int i = 0; i < files.length; i++){
            Offsets[i] = outputFile.getFilePointer();
            RandomAccessFile inputFile = new RandomAccessFile(files[i],"r");
            byte[] input = new byte[(int)inputFile.length()];
            inputFile.read(input);
            new TXGHeader(outputFile, new TPL(new ByteStream(input)));
        }
    }

    public static void unpack(String inputFile, String outputDir) throws IOException, DataFormatException {
        ByteStream reader = new ByteStream(Files.readAllBytes(Paths.get(inputFile)));
        FileCount = reader.readWord();
        Offsets = new long[FileCount];
        for (int i = 0; i < FileCount; i++){
            Offsets[i] = reader.readWord();
        }

        for (int i = 0; i < FileCount; i++){
            reader.seek((int)Offsets[i]);
            Path fileName = Paths.get(outputDir,i+".tpl");
            System.out.println(String.format("File Name: {0}", fileName));
            TXGHeader header = new TXGHeader(reader);
            RandomAccessFile raf = new RandomAccessFile(fileName.toString(),"rw");
            try {
                new TPL(header, raf);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class TPL {
        private final long _Identifier = 0x0020AF30;
        public int ImageCount;
        public int TableOffset;

        public int[] ITableOffsets;
        public int[] PTableOffsets;

        public int EntryCount;
        private int _Padding;
        public long PaletteFormat;
        public long PaletteOffset;

        public int Height;
        public int Width;
        public long ImageFormat;
        public int ImageOffset;
        public long WrapS;
        public long WrapT;
        public long MinFilter;
        public long MagFilter;
        public float LODBias;
        public char EnableEdgeLOD;
        public char MinLOD;
        public char MaxLOD;
        private char _Unpacked;
        public ImageDataFormat _Format;

        public byte[][] ImageData;
        public byte[][] PaletteData;

        public TPL(ByteStream stream) throws IOException, DataFormatException {
            if (stream.readWord() != _Identifier)
                throw new DataFormatException();
            ImageCount = stream.readWord();
            TableOffset = stream.readWord();

            ITableOffsets = new int[ImageCount];
            PTableOffsets = new int[ImageCount];

            for (int i = 0; i < ImageCount; i++){
                ITableOffsets[i] = stream.readWord();
                PTableOffsets[i] = stream.readWord();
            }

            ImageData = new byte[ImageCount][];
            PaletteData = new byte[ImageCount][];
            for (int i = 0; i < ImageCount; i++){
                stream.seek(ITableOffsets[i]);
                Height = stream.readShort();
                Width = stream.readShort();
                ImageFormat = stream.readWord();
                ImageOffset = stream.readWord();
                WrapS = stream.readWord();
                WrapT = stream.readWord();
                MinFilter = stream.readWord();
                MagFilter = stream.readWord();
                LODBias = stream.readFloat();
                EnableEdgeLOD = stream.readByte();
                MinLOD = stream.readByte();
                MaxLOD = stream.readByte();
                _Unpacked = stream.readByte();
                _Format = ImageDataFormat.GetFormat((int)ImageFormat);
                int imageSize = _Format.CalculateDataSize(Width,Height);
                stream.seek(ImageOffset);
                ImageData[i] = stream.readBytes(imageSize);
            }
        }

        public TPL(TXGHeader header, RandomAccessFile writer) throws IOException {
            writer.write(ByteUtils.fromUint32(_Identifier));
            writer.write(ByteUtils.fromInt32(header.ImageCount.intValue()));
            writer.write(ByteUtils.fromInt32(0x0C));

            ITableOffsets = new int[header.ImageCount.intValue()];
            PTableOffsets = new int[header.ImageCount.intValue()];

            for (int i = 0; i < header.ImageCount.intValue(); i++){
                writer.write(ByteUtils.fromInt32(0));
                writer.write(ByteUtils.fromInt32(0));
            }

            if (header._Format.hasPalette){
                for (int i = 0; i < header.ImageCount.intValue(); i++){
                    try {
                        PTableOffsets[i] = (int) writer.getFilePointer();
                        writer.write(ByteUtils.fromInt16(header.PaletteData[i].length / 2));
                        writer.write(ByteUtils.fromInt16(0));
                        writer.write(ByteUtils.fromInt32(header.PaletteFormat.intValue()));
                        writer.write(ByteUtils.fromInt32(0));
                        ByteUtils.byteAlign(writer, 0x20);
                        long tmp = writer.getFilePointer();
                        System.err.println(String.format("Place to jump back to: %i",tmp));
                        writer.seek(PTableOffsets[i] + 0x08);
                        writer.write(ByteUtils.fromUint32(tmp));
                        writer.seek(tmp);
                        writer.write(header.PaletteData[i]);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println(String.format("Palette for image number: %d",i));
                    }
                }
            }

            System.err.println(String.format("ImageCount: %d",header.ImageCount.intValue()));

            for (int i = 0; i < header.ImageCount.intValue(); i++){
                ITableOffsets[i] = (int)writer.getFilePointer();
                System.err.println(String.format("ITableOffset %d: %d",i,ITableOffsets[i]));
                writer.write(ByteUtils.fromUint16(header.Height.shortValue()));
                writer.write(ByteUtils.fromUint16(header.Width.shortValue()));
                writer.write(ByteUtils.fromInt32(header.ImageFormat.intValue()));
                long PastOffset = writer.getFilePointer();
                System.err.println(String.format("PastOffset: %d",PastOffset));
                writer.write(ByteUtils.fromInt32(0));
                writer.write(ByteUtils.fromInt32(0));
                writer.write(ByteUtils.fromInt32(0));
                writer.write(ByteUtils.fromInt32(1));
                writer.write(ByteUtils.fromInt32(1));
                writer.write(ByteUtils.fromInt32(0));
                writer.write(ByteUtils.fromInt32(0));
                ByteUtils.byteAlign(writer,0x20);
                //writer.seek(writer.getFilePointer()-4);
                long tmp = writer.getFilePointer();
                System.err.println(String.format("PastOffset: %d\nPlace to jump back to: %d",PastOffset,tmp));
                writer.seek(PastOffset);
                writer.write(ByteUtils.fromUint32(tmp));
                writer.write(header.ImageData[i]);
            }

            writer.seek(0x0C);
            for (int i = 0; i < header.ImageCount.intValue(); i++){
                System.err.println(String.format("ITableOffset %d: %02x\nPTableOffset %d: %02x",i,ITableOffsets[i],i,PTableOffsets[i]));
                writer.write(ByteUtils.fromInt32(ITableOffsets[i]));
                writer.write(ByteUtils.fromInt32(PTableOffsets[i]));
            }
            writer.close();
        }
    }

    private static class TXGHeader {
        public UnsignedInteger ImageCount;
        public UnsignedInteger ImageFormat;
        public UnsignedInteger PaletteFormat;
        public UnsignedInteger Width;
        public UnsignedInteger Height;
        public UnsignedInteger SingleImage;
        public long[] ImageOffsets;
        public long[] PaletteOffsets;

        public byte[][] ImageData;
        public byte[][] PaletteData;
        public ImageDataFormat _Format;

        public TXGHeader(RandomAccessFile raf, TPL tpl) throws IOException {
            ByteUtils.writeUint32(raf,tpl.ImageCount);
            ByteUtils.writeUint32(raf,tpl.ImageFormat);
            ByteUtils.writeUint32(raf,tpl.PaletteFormat);
            ByteUtils.writeUint32(raf,tpl.Width);
            ByteUtils.writeUint32(raf,tpl.Height);
            if(tpl.ImageCount != 1)
                ByteUtils.writeInt32(raf,0);
            else
                ByteUtils.writeInt32(raf,1);

            ImageOffsets = new long[tpl.ImageCount];
            PaletteOffsets = new long[tpl.ImageCount];

            long PastOffset = raf.getFilePointer();
            for (int i = 0; i < tpl.ImageData.length; i++){
                ByteUtils.writeInt32(raf,0);
            }

            for (int i = 0; i < tpl.PaletteData.length; i++){
                ByteUtils.writeInt32(raf,0);
            }
            ByteUtils.byteAlign(raf,0x20);

            for (int i = 0; i < tpl.ImageData.length; i++){
                ImageOffsets[i] = raf.getFilePointer();
                raf.write(tpl.ImageData[i]);
            }

            try{
                for (int i = 0; i < tpl.PaletteData.length; i++){
                    PaletteOffsets[i] = raf.getFilePointer();
                    raf.write(tpl.PaletteData[i]);
                }
            } catch (IllegalArgumentException iae) {
                System.out.println(iae.getMessage());
            }

            long CurrentOffset = raf.getFilePointer();
            ByteUtils.writeInt32(raf,(int)PastOffset);

            for (int i = 0; i < tpl.ImageData.length; i++){
                ByteUtils.writeUint32(raf,ImageOffsets[i]);
            }

            for (int i = 0; i < tpl.PaletteData.length; i++){
                ByteUtils.writeUint32(raf,PaletteOffsets[i]);
            }
            raf.seek(CurrentOffset);
            raf.close();
        }

        public TXGHeader(ByteStream reader) throws IOException, DataFormatException {
            ImageCount = UnsignedInteger.fromIntBits(reader.readWord());  //2
            ImageFormat = UnsignedInteger.fromIntBits(reader.readWord()); //E
            PaletteFormat = UnsignedInteger.fromIntBits(reader.readWord());//0
            Width = UnsignedInteger.fromIntBits(reader.readWord());         //40
            Height = UnsignedInteger.fromIntBits(reader.readWord());        //40
            SingleImage = UnsignedInteger.fromIntBits(reader.readWord());   //0
            _Format = ImageDataFormat.GetFormat(ImageFormat.intValue());

            ImageData = new byte[ImageCount.intValue()][];
            PaletteData = new byte[ImageCount.intValue()][];

            for (int i = 0; i < ImageCount.intValue(); i++){
                int ImageSize = _Format.CalculateDataSize(Width.longValue(),Height.longValue());
                int pos = reader.offset();
                UnsignedInteger imagePos = UnsignedInteger.fromIntBits(reader.readWord());  //40
                System.err.println(String.format("ImageSize: %d\nimagePos: %d",ImageSize,imagePos.longValue()));
                reader.seek(imagePos.intValue());
                ImageData[i] = reader.readBytes(ImageSize);
                reader.seek(pos);
            }

            if (_Format.hasPalette) {
                for (int i = 0; i < ImageCount.intValue(); i++) {
                    int paletteLength = reader.readWord();
                    if (paletteLength != -1) {
                        int pos = reader.offset();
                        reader.seek(paletteLength);
                        PaletteData[i] = reader.readBytes(0x200);
                        reader.seek(pos);
                    } else {
                        int previousPaletteLength = PaletteData[i-1].length;
                        PaletteData[i] = Arrays.copyOf(PaletteData[i-1],previousPaletteLength);
                    }
                }
            }
        }
    }

    private static class ImageDataFormat {
        public String name;
        public boolean hasPalette;
        public long bitsPerPixel;
        public long blockWidth;
        public long blockHeight;

        public static final ImageDataFormat I4 = new ImageDataFormat("I4", false, 4, 8, 8);
        public static final ImageDataFormat I8 = new ImageDataFormat("I8", false, 8, 8, 4);
        public static final ImageDataFormat IA4 = new ImageDataFormat("IA4", false, 8, 8, 4);
        public static final ImageDataFormat IA8 = new ImageDataFormat("IA8", false, 16, 4, 4);
        public static final ImageDataFormat RGB565 = new ImageDataFormat("RGB565", false, 16, 4, 4);
        public static final ImageDataFormat RGB5A3 = new ImageDataFormat("RGB5A3", false, 16, 4, 4);
        public static final ImageDataFormat RGBA8 = new ImageDataFormat("RGBA8", false, 32, 4, 4);
        public static final ImageDataFormat C4 = new ImageDataFormat("C4", true, 4, 8, 8);
        public static final ImageDataFormat C8 = new ImageDataFormat("C8", true, 8, 8, 4);
        public static final ImageDataFormat C14X2 = new ImageDataFormat("C14X2", true, 16, 4, 4);
        public static final ImageDataFormat CMPR = new ImageDataFormat("CMPR", false, 4, 4, 8);

        public ImageDataFormat(String name, boolean hasPalette, int bitsPerPixel, int blockWidth, int blockHeight){
            this.name = name;
            this.hasPalette = hasPalette;
            this.bitsPerPixel = bitsPerPixel;
            this.blockWidth = blockWidth;
            this.blockHeight = blockHeight;
        }

        public static ImageDataFormat GetFormat(int formatID) throws DataFormatException {
            switch (formatID)
            {
                case 0x00: return I4;
                case 0x01: return I8;
                case 0x02: return IA4;
                case 0x03: return IA8;
                case 0x04: return RGB565;
                case 0x05: return RGB5A3;
                case 0x06: return RGBA8;
                case 0x08: return C4;
                case 0x09: return C8;
                case 0x0A: return C14X2;
                case 0x0E: return CMPR;
                default: throw new DataFormatException();
            }
        }
        public int CalculateDataSize(long width, long height)
        {
            return (int)(RoundWidth(width) * RoundHeight(height) * bitsPerPixel >> 3);
        }

        private long RoundWidth(long width)
        {
            return width + ((blockWidth - (width % blockWidth)) % blockWidth);
        }

        private long RoundHeight(long height)
        {
            return height + ((blockHeight - (height % blockHeight)) % blockHeight);
        }
    }
}
