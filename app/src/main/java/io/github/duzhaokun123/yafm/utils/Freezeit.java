package io.github.duzhaokun123.yafm.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Freezeit {
    private static final String TAG = "Freezeit";

    public final static int CFG_TERMINATE = 10, CFG_SIGSTOP = 20, CFG_FREEZER = 30, CFG_WHITELIST = 40, CFG_WHITEFORCE = 50;

    // 获取信息 无附加数据 No additional data required
    public final static byte getStatus = 1;       // return string: "Freezeit is running"
    public final static byte getInfo = 2;         // return string: "ID\nName\nVersion\nVersionCode\nAuthor"
    public final static byte getChangelog = 3;    // return string: "changelog"
    public final static byte getLog = 4;          // return string: "log"
    public final static byte getAppCfg = 5;       // return string: "package x\n..."   "包名 配置号\n..."
    public final static byte getRealTimeInfo = 6; // return bytes[variable]: (rawBitmap 内存 频率 使用率 电流)
    public final static byte getProcessInfo = 7;  // return string: "process cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\n..."
    public final static byte getSettings = 8;     // return bytes[256]: all settings parameter
    public final static byte getUidTime = 9;      // return "uid time_micro_seconds increase_micro_seconds\n..."

    // 设置 需附加数据
    public final static byte setAppCfg = 21;      // send "package x\n..."   "包名 配置号\n..."
    public final static byte setAppLabel = 22;    // send "uid label\nuid label\nuid label\n..."
    public final static byte setSettingsVar = 23; // send bytes[2]: [0]index [1]value

    // 进程管理 需附加数据
    public final static byte killPid = 41;        // send string: "1234"  //pid num
    public final static byte killApp = 42;        // send string: "packageName"
    public final static byte discharged = 43;     // send string: "packageName"  //临时放行后台

    // 其他命令 无附加数据 No additional data required
    public final static byte clearLog = 61;         // return string: "log" //清理并返回log
    public final static byte printFreezerProc = 62; // return string: "log" //打印冻结状态进程并返回log

    public final static byte reboot = 81;
    public final static byte rebootRecovery = 82;
    public final static byte rebootBootloader = 83;
    public final static byte rebootEdl = 84;

    public static synchronized void freezeitTask(byte command, byte[] AdditionalData, Handler handler) {
        // dataHeader[0-3]:附带数据大小(uint32 小端)
        // dataHeader[4]: 命令(可参考上面)
        // dataHeader[5]: 附带数据的异或校验值
        byte[] dataHeader = {0, 0, 0, 0, command, 0};
        byte[] responseBuf = null;
        try {
            var socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 60613), 3000);

            // 终端执行 setenforce 0 ，即设置 SELinux 为宽容模式, 普通安卓应用才可以使用 LocalSocket
            // var socket = new LocalSocket();
            // socket.connect(new LocalSocketAddress("FreezeitServer", LocalSocketAddress.Namespace.ABSTRACT));

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            if (AdditionalData != null && AdditionalData.length > 0) {
                byte XOR = 0;
                for (byte b : AdditionalData)
                    XOR ^= b;

                Int2Byte(AdditionalData.length, dataHeader, 0);
                dataHeader[5] = XOR;

                os.write(dataHeader);
                os.write(AdditionalData);
            } else {
                os.write(dataHeader);
            }

            os.flush();

            if (handler != null) {
                int receiveLen = is.read(dataHeader, 0, 6);
                if (receiveLen != 6) {
                    Log.e(TAG, "Receive dataHeader Fail, receiveLen:" + receiveLen);
                    socket.close();
                    return;
                }

                int payloadLen = Byte2Int(dataHeader, 0);

                responseBuf = new byte[payloadLen];

                int readCnt = 0;
                while (readCnt < payloadLen) { //欲求不满
                    int cnt = is.read(responseBuf, readCnt, payloadLen - readCnt);
                    if (cnt < 0) {
                        Log.e(TAG, "Get payload Fail");
                        socket.close();
                        return;
                    }
                    readCnt += cnt;
                }
            }
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (handler != null) {
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putByteArray("response", responseBuf);
            msg.setData(data);
            handler.sendMessage(msg);
        }
    }

    // 小端 只是避免内存越界，不处理转换失败的情况
    public static int Byte2Int(byte[] bytes, int byteOffset) {
        if (bytes == null || (byteOffset + 4) > bytes.length)
            return 0;

        return Byte.toUnsignedInt(bytes[byteOffset]) |
                (Byte.toUnsignedInt(bytes[byteOffset + 1]) << 8) |
                (Byte.toUnsignedInt(bytes[byteOffset + 2]) << 16) |
                (Byte.toUnsignedInt(bytes[byteOffset + 3]) << 24);
    }

    public static void Byte2Int(byte[] bytes, int byteOffset, int byteLength, int[] ints, int intOffset) {
        if (ints == null || bytes == null || (intOffset + byteLength / 4) > ints.length ||
                (byteOffset + byteLength) > bytes.length)
            return;

        for (int byteIdx = byteOffset; byteIdx < byteOffset + byteLength; byteIdx += 4) {
            ints[intOffset++] = Byte.toUnsignedInt(bytes[byteIdx]) |
                    (Byte.toUnsignedInt(bytes[byteIdx + 1]) << 8) |
                    (Byte.toUnsignedInt(bytes[byteIdx + 2]) << 16) |
                    (Byte.toUnsignedInt(bytes[byteIdx + 3]) << 24);
        }
    }

    public static void Int2Byte(int value, byte[] bytes, int byteOffset) {
        if (bytes == null) return;
        if ((byteOffset + 4) > bytes.length) {
            while (byteOffset < bytes.length)
                bytes[byteOffset++] = 0;
            return;
        }

        bytes[byteOffset++] = (byte) value;
        bytes[byteOffset++] = (byte) (value >> 8);
        bytes[byteOffset++] = (byte) (value >> 16);
        bytes[byteOffset] = (byte) (value >> 24);
    }

    public static Bitmap resize(Bitmap bitmap, float scaleX, float scaleY) {
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }
}
