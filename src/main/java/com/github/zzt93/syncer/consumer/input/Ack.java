package com.github.zzt93.syncer.consumer.input;

import com.github.zzt93.syncer.common.thread.NotThreadSafe;
import com.github.zzt93.syncer.common.thread.ThreadSafe;
import com.github.zzt93.syncer.config.syncer.SyncerMysql;
import com.github.zzt93.syncer.producer.input.connect.BinlogInfo;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zzt
 */
public class Ack {
  private static final Logger logger = LoggerFactory.getLogger(Ack.class);

  private final String metaDir;
  @ThreadSafe(sharedBy = {"main", "shutdown hook"}, des = "it is fixed before shutdown hook thread start")
  private final Map<String, Path> connectorMetaPath = new HashMap<>();
  @ThreadSafe(sharedBy = {"main", "shutdown hook"}, des = "it is fixed before shutdown hook thread start")
  private final HashMap<String, BinlogInfo> binlogInfos = new HashMap<>();
  private final String clientId;


  public Ack(String clientId, SyncerMysql syncerMysql) {
    this.clientId = clientId;
    this.metaDir = syncerMysql.getLastRunMetadataDir();
  }

  List<String> connectorMeta(String identifier) {
    BinlogInfo binlogInfo = this.binlogInfos.get(identifier);
    return Lists.newArrayList(binlogInfo.getBinlogFilename(), "" + binlogInfo.getBinlogPosition());
  }

  Map<String, Path> connectorMetaPath() {
    return connectorMetaPath;
  }

  @NotThreadSafe
  BinlogInfo addDatasource(String identifier) {
    Path path = Paths.get(metaDir, clientId, identifier);
    connectorMetaPath.put(identifier, path);
    BinlogInfo binlogInfo = new BinlogInfo();
    if (!Files.exists(path)) {
      logger.info("Last run meta file not exists, fresh run");
    } else {
      List<String> lines;
      try {
        lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() == 2) {
          binlogInfo = new BinlogInfo(lines.get(0), Long.parseLong(lines.get(1)));
          binlogInfos.put(identifier, binlogInfo);
        } else {
          logger.warn("Meta file in {} crashed, take as fresh run", connectorMetaPath);
        }
      } catch (IOException ignored) {
        logger.error("Impossible to run to here", ignored);
      }
    }
    return binlogInfo;
  }
}
