package uk.org.wolfpuppy.minecraft.chunkchecker;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;
import uk.org.wolfpuppy.minecraft.chunkchecker.util.DimChunkPos;
import uk.org.wolfpuppy.minecraft.chunkchecker.util.LimitedMap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@Mod(modid = "chunkchecker",
        useMetadata = true,
        serverSideOnly = true,
        acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber
public class ChunkChecker {
    private static final int UNLOADED_CHUNK_CACHE = 100;
    private static final int RELOAD_COUNT = 3;

    private static Logger logger;
    private static boolean serverStarted = false;

    static class ChunkLoadInfo {
        private DimChunkPos pos;
        private List<Throwable> loadTraces = new ArrayList<>(RELOAD_COUNT);

        public ChunkLoadInfo(DimChunkPos pos) {
            this.pos = pos;
        }

        public boolean addTraceAndCheckLimit(Throwable t) {
            loadTraces.add(t);
            if (loadTraces.size() >= RELOAD_COUNT) {
                logger.warn("Chunk at {} has reloaded too many times, most recent trace: {}",
                        pos, stackTraceAsString(t));
                return true;
            }
            return false;
        }

        private static String stackTraceAsString(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            return sw.toString();
        }
    }

    private static Map<DimChunkPos, ChunkLoadInfo> loadedChunks = new HashMap<>();
    private static Map<DimChunkPos, ChunkLoadInfo> unloadedChunks = new LimitedMap<>(UNLOADED_CHUNK_CACHE);
    private static Set<DimChunkPos> ignoredChunks = new HashSet<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {}

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        serverStarted = true;
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        Chunk chunk = event.getChunk();
        WorldServer world = (WorldServer) chunk.getWorld();
        int dim = world.provider.getDimension();
        DimChunkPos pos = new DimChunkPos(dim, chunk.getPos());

        if (!serverStarted || chunk.getWorld().isSpawnChunk(pos.x, pos.z) || ignoredChunks.contains(pos)) {
            // spawn chunks don't unload, so loading them is boring. ignored chunks are.. ignored.
            return;
        }

        ChunkLoadInfo loadInfo = unloadedChunks.remove(pos);

        if (world.getPlayerChunkMap().contains(pos.x, pos.z)) {
            // chunk loading because of player presence, so this is okay, but we need to keep the chunk record
            // if it already existed, since it may have previously been loaded by a non-player event.
            if (loadInfo != null) {
                loadedChunks.put(pos, loadInfo);
            }
            return;
        }

        // make a new record if it didn't exist.
        if (loadInfo == null) {
            loadInfo = new ChunkLoadInfo(pos);
        }

        if (loadInfo.addTraceAndCheckLimit(new Throwable())) {
            // limit was hit, stop paying attention to this chunk and let the record drop.
            ignoredChunks.add(pos);
        } else {
            // limit not hit, keep the record around.
            loadedChunks.put(pos, loadInfo);
        }
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        Chunk chunk = event.getChunk();
        WorldServer world = (WorldServer) chunk.getWorld();
        int dim = world.provider.getDimension();
        DimChunkPos pos = new DimChunkPos(dim, chunk.getPos());

        // just move the record back to the unloaded cache if it exists.
        ChunkLoadInfo loadInfo = loadedChunks.remove(pos);
        if (loadInfo != null) {
            unloadedChunks.put(pos, loadInfo);
        }
    }
}
