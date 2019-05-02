package unimelb.bitbox.service;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static unimelb.bitbox.util.RequestUtil.*;

public class FileServiceImpl implements FileService {
    private static Logger log = Logger.getLogger(FileService.class.getName());
    private SocketService socketService = new SocketServiceImpl();

    private Base64.Encoder encoder = Base64.getEncoder();
    private Base64.Decoder deCoder = Base64.getDecoder();

    private static Map<String, Integer> retryCount = new HashMap<>();

    @Override
    public Document requestGenerate(FileSystemEvent fileSystemEvent) {
        Document request = new Document();
        if (fileSystemEvent.event == EVENT.FILE_CREATE) {
            request.append("command", FILE_CREATE_REQUEST);
            request.append("fileDescriptor", fileSystemEvent.fileDescriptor.toDoc());
            request.append("pathName", fileSystemEvent.pathName);
        } else if (fileSystemEvent.event == EVENT.FILE_DELETE) {
            request.append("command", FILE_DELETE_REQUEST);
            request.append("fileDescriptor", fileSystemEvent.fileDescriptor.toDoc());
            request.append("pathName", fileSystemEvent.pathName);
        } else if (fileSystemEvent.event == EVENT.DIRECTORY_CREATE) {
            request.append("command", DIRECTORY_CREATE_REQUEST);
            request.append("pathName", fileSystemEvent.pathName);
        } else if (fileSystemEvent.event == EVENT.DIRECTORY_DELETE) {
            request.append("command", DIRECTORY_DELETE_REQUEST);
            request.append("pathName", fileSystemEvent.pathName);
        } else if (fileSystemEvent.event == EVENT.FILE_MODIFY) {
            request.append("command", FILE_MODIFY_REQUEST);
            request.append("fileDescriptor", fileSystemEvent.fileDescriptor.toDoc());
            request.append("pathName", fileSystemEvent.pathName);
        } else {
            return null;
        }
        return request;
    }

    @Override
    public Document OperateAndResponseGenerate(Socket socket, Document request, FileSystemManager fileSystemManager) {
        Document response = new Document();
        try {
            switch (request.getString("command")) {
                case FILE_CREATE_REQUEST:
                    response.append("command", FILE_CREATE_RESPONSE);
                    Document fileDescriptor = (Document) request.get("fileDescriptor");
                    String pathName = request.getString("pathName");
                    response.append("fileDescriptor", fileDescriptor);
                    response.append("pathName", pathName);
                    if (fileSystemManager.isSafePathName(pathName) && !fileSystemManager.fileNameExists(pathName)) {
                        if (fileSystemManager.createFileLoader(pathName, fileDescriptor.getString("md5"),
                                fileDescriptor.getLong("fileSize"), fileDescriptor.getLong("lastModified"))) {
                            //request byte and write
                            if (!fileSystemManager.checkShortcut(pathName)) {
                                getByte(socket, request, 0);
                            } else {
                                fileSystemManager.writeFile(pathName,
                                        fileSystemManager.readFile(fileDescriptor.getString("md5"), 0, fileDescriptor.getLong("fileSize")), 0);
                            }
                            response.append("message", "file loader ready");
                            response.append("status", true);
                        } else {
                            response.append("message", "file loader create fail");
                            response.append("status", false);
                        }
                    } else {
                        response.append("message", "the path name exist");
                        response.append("status", false);
                    }
                    break;
                case FILE_MODIFY_REQUEST:
                    response.append("command", FILE_MODIFY_RESPONSE);
                    fileDescriptor = (Document) request.get("fileDescriptor");
                    pathName = request.getString("pathName");
                    response.append("fileDescriptor", fileDescriptor);
                    response.append("pathName", pathName);
                    if (fileSystemManager.fileNameExists(pathName)) {
                        if (fileSystemManager.modifyFileLoader(pathName, fileDescriptor.getString("md5"), fileDescriptor.getLong("lastModified"))) {
                            if (!fileSystemManager.checkShortcut(pathName)) {
                                getByte(socket, request, 0);
                            } else {
                                fileSystemManager.writeFile(pathName,
                                        fileSystemManager.readFile(fileDescriptor.getString("md5"), 0, fileDescriptor.getLong("fileSize")), 0);
                            }
                        } else {
                            response.append("message", "file loader create fail");
                            response.append("status", false);
                        }
                    } else {
                        response.append("message", "no such file can modify");
                        response.append("status", false);
                    }
                    break;
                case FILE_BYTES_REQUEST:
                    Document descriptor = (Document) request.get("fileDescriptor");
                    long position = request.getLong("position");
                    long length = request.getLong("length");

                    response.append("command", FILE_BYTES_RESPONSE);
                    response.append("fileDescriptor", descriptor);
                    response.append("pathName", request.getString("pathName"));
                    response.append("position", position);
                    response.append("length", length);
                    try {
                        ByteBuffer byteBuffer = fileSystemManager.readFile(descriptor.getString("md5"), position, length);
                        response.append("content", byteBufferToString(byteBuffer));
                        response.append("message", "successful read");
                        response.append("status", true);

                    } catch (Exception ex) {
                        response.append("message", ex.getMessage());
                        response.append("status", false);
                    }

                    break;
                case FILE_DELETE_REQUEST:
                    fileDescriptor = (Document) request.get("fileDescriptor");
                    pathName = request.getString("pathName");
                    response.append("command", FILE_DELETE_RESPONSE);
                    response.append("fileDescriptor", fileDescriptor);
                    response.append("pathName", pathName);
                    if (fileSystemManager.isSafePathName(pathName) && fileSystemManager.fileNameExists(pathName, fileDescriptor.getString("md5"))) {
                        if (fileSystemManager.deleteFile(pathName, fileDescriptor.getLong("lastModified"), fileDescriptor.getString("md5"))) {
                            log.info("delete file:" + pathName);
                            response.append("message", "successful delete");
                            response.append("status", true);
                        } else {
                            response.append("message", "delete fail");
                            response.append("status", false);
                        }
                    } else {
                        response.append("message", "pathname does not exist");
                        response.append("status", false);
                    }
                    break;
                case DIRECTORY_CREATE_REQUEST:
                    pathName = request.getString("pathName");
                    response.append("command", DIRECTORY_CREATE_RESPONSE);
                    response.append("pathName", pathName);
                    if (fileSystemManager.isSafePathName(pathName) && !fileSystemManager.dirNameExists(pathName)) {
                        if (fileSystemManager.makeDirectory(pathName)) {
                            response.append("message", "successful create");
                            response.append("status", true);
                        } else {
                            response.append("message", "failed create");
                            response.append("status", false);
                        }
                    } else {
                        response.append("message", "pathname already exists");
                        response.append("status", false);
                    }
                    break;
                case DIRECTORY_DELETE_REQUEST:
                    pathName = request.getString("pathName");
                    response.append("command", DIRECTORY_DELETE_RESPONSE);
                    if (fileSystemManager.isSafePathName(pathName) && fileSystemManager.dirNameExists(pathName)) {
                        if (fileSystemManager.deleteDirectory(pathName)) {
                            response.append("message", "success delete directory");
                            response.append("status", true);
                        } else {
                            response.append("message", "fail delete directory");
                            response.append("status", false);
                        }
                    } else {
                        response.append("message", "there are no such directory");
                        response.append("status", false);
                    }
                    break;
                case FILE_BYTES_RESPONSE:
                    position = request.getLong("position");
                    pathName = request.getString("pathName");
                    if (!request.getBoolean("status")) {
                        Integer count = retryCount.get(pathName);
                        //try again if never try
                        if(count != null && count != 0){
                            retryCount.put(pathName, 1);
                            //try again
                            getByte(socket, request, position);
                        }
                        log.warning("byte request fail, retry times: " + count);
                    } else {
                        length = request.getLong("length");
                        //write into file
                        fileSystemManager.writeFile(pathName, deCoder.decode(ByteBuffer.wrap(request.getString("content").getBytes())),
                                request.getLong("position"));
                        if (!fileSystemManager.checkWriteComplete(pathName)) {
                            getByte(socket, request, position + length);
                        }
                    }
                case DIRECTORY_CREATE_RESPONSE:
                case DIRECTORY_DELETE_RESPONSE:
                case FILE_CREATE_RESPONSE:
                case FILE_DELETE_RESPONSE:
                case FILE_MODIFY_RESPONSE:
                case HANDSHAKE_REQUEST:
                case HANDSHAKE_RESPONSE:
                    //do nothing
                    return null;
                default:
                    //no such command we need stop the socket
                    log.warning("no such kind of command");
                    response.append("command", INVALID_PROTOCOL);
                    response.append("message", "message must contain a command field as string");
//                    socket.close();
                    break;
            }
        } catch (NoSuchAlgorithmException e) {
            log.warning(e.getMessage());
            return null;
        } catch (IOException e) {
            log.warning(e.getMessage());
            return null;
        } catch (Exception ex) {
            log.warning(ex.getMessage());
            return null;
        }
        return response;
    }

    private void getByte(Socket socket, Document request, long position) throws Exception {
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String pathName = request.getString("pathName");
        Document byteRequest = generateByteRequest(fileDescriptor, pathName, position);
        socketService.send(socket, byteRequest.toJson());
        log.info(byteRequest.toJson());
    }

    private Document generateByteRequest(Document descriptor, String pathName, long position) throws Exception {
        long buffer = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
        long fileSize = descriptor.getLong("fileSize");
        long length = (fileSize > buffer + position) ? buffer : (fileSize - position);
        Document document = new Document();
        document.append("command", FILE_BYTES_REQUEST);
        document.append("fileDescriptor", descriptor);
        document.append("pathName", pathName);
        document.append("position", position);
        document.append("length", length);

        return document;
    }

    private String byteBufferToString(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        ByteBuffer buffer = encoder.encode(byteBuffer);
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
        return charBuffer.toString();
    }

}
