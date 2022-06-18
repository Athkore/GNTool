package com.github.nicholasmoser.graphics;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

        outputFile.seek(0x04);
        for (int i = 0; i < files.length; i++) {
            ByteUtils.writeUint32(outputFile, Offsets[i]);
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
            System.out.println(String.format("File Name: %s", fileName));
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
        public byte EnableEdgeLOD;
        public byte MinLOD;
        public byte MaxLOD;
        private byte _Unpacked;
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
                System.err.println(String.format("Current image: %d\nTotal nr of images: %d\nImageOffset: %d\nCurrent Offset: %d\nTotal size: %d",i,ImageCount,ITableOffsets[i],stream.offset(),stream.length()));
                Height = stream.readShort();
                Width = stream.readShort();
                ImageFormat = stream.readWord();
                System.err.println(String.format("Image Format: 0x%02X",ImageFormat));
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
                        writer.seek(PTableOffsets[i] + 0x08);
                        writer.write(ByteUtils.fromUint32(tmp));
                        writer.seek(tmp);
                        System.err.print("[");
                        for (byte b : header.PaletteData[i]) {
                            System.err.print(String.format("%02x,",b));
                        }
                        System.err.println("]");
                        System.err.println(header.PaletteData[i].toString());
                        writer.write(header.PaletteData[i]);
                        System.err.println(writer.getFilePointer());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println(String.format("Palette for image number: %d",i));
                    }
                }
            }

            System.err.println(String.format("ImageCount: %d",header.ImageCount.intValue()));

            for (int i = 0; i < header.ImageCount.intValue(); i++){
                System.err.println(writer.getFilePointer());
                ITableOffsets[i] = (int)writer.getFilePointer();
                System.err.println(String.format("ITableOffset %d: %d",i,ITableOffsets[i]));
                writer.write(ByteUtils.fromUint16(header.Height.shortValue()));
                writer.write(ByteUtils.fromUint16(header.Width.shortValue()));
                writer.write(ByteUtils.fromInt32(header.ImageFormat.intValue()));
                long PastOffset = writer.getFilePointer();
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
                writer.seek(tmp);
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

            try {
                for (int i = 0; i < tpl.PaletteData.length; i++) {
                        PaletteOffsets[i] = raf.getFilePointer();
                        raf.write(tpl.PaletteData[i]);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            long CurrentOffset = raf.getFilePointer();
            raf.seek(PastOffset);

            for (int i = 0; i < tpl.ImageData.length; i++){
                ByteUtils.writeUint32(raf,ImageOffsets[i]);
            }
            for (int i = 0; i < tpl.PaletteData.length; i++) {
                ByteUtils.writeUint32(raf, PaletteOffsets[i]);
            }

            raf.seek(CurrentOffset);
        }

        public TXGHeader(ByteStream reader) throws IOException, DataFormatException {
            ImageCount = UnsignedInteger.fromIntBits(reader.readWord());  //1
            ImageFormat = UnsignedInteger.fromIntBits(reader.readWord()); //9
            PaletteFormat = UnsignedInteger.fromIntBits(reader.readWord());//2
            Width = UnsignedInteger.fromIntBits(reader.readWord());         //64
            Height = UnsignedInteger.fromIntBits(reader.readWord());        //26
            SingleImage = UnsignedInteger.fromIntBits(reader.readWord());   //1
            _Format = ImageDataFormat.GetFormat(ImageFormat.intValue());

            ImageData = new byte[ImageCount.intValue()][];
            PaletteData = new byte[ImageCount.intValue()][];
            int ImageSize = _Format.CalculateDataSize(Width.longValue(),Height.longValue());
            for (int i = 0; i < ImageCount.intValue(); i++){
                int imagePos = reader.readWord();  //40
                int returnPos = reader.offset();
                reader.seek(imagePos);
                ImageData[i] = reader.readBytes(ImageSize);
                reader.seek(returnPos);
            }

            if (_Format.hasPalette) {
                for (int i = 0; i < ImageCount.intValue(); i++) {
                    int paletteLength = reader.readWord();
                    System.err.println(String.format("palleteLength: %02x",paletteLength));
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
