package com.robvanblerk.tieredpower.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.robvanblerk.tieredpower.energy.PowerInfo;
import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.StorageActionPacket;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * Everything in the storage network: items, fluids and gases. Click: take a stack. Right-click: take half.
 * Shift-click: into your inventory. Click with an item held to store it (right-click stores one).
 * Holding a bucket or tank: click a fluid to fill it, or click anywhere else to pour it into storage.
 * Search by name, or "@mod" to search by mod.
 */
public class StorageTerminalScreen<T extends StorageTerminalMenu> extends AbstractContainerScreen<T> {
	protected static final int COLS = 9, GRID_X = 8, GRID_Y = 20, BAR_X = 174;
	protected final int ROWS;

	/** One cell of the grid: an item or a fluid. */
	private record Row(ItemStack item, FluidStack fluid, long count, String name, boolean craftable) {
		boolean isFluid() {
			return !fluid.isEmpty();
		}
	}

	private static final String[] VIEWS = {"All", "Items", "Fluids"};
	private EditBox search;
	private boolean sortByName;
	private int view; // 0 all, 1 items, 2 fluids
	private int scrollRow;

	private Object cacheItems, cacheFluids, cacheCraftables;
	private String cacheSearch = "";
	private boolean cacheSort;
	private int cacheView;
	private List<Row> visible = List.of();

	public StorageTerminalScreen(T menu, Inventory inventory, Component title) {
		this(menu, inventory, title, 5, 222);
	}

	protected StorageTerminalScreen(T menu, Inventory inventory, Component title, int rows, int height) {
		super(menu, inventory, title);
		this.ROWS = rows;
		imageWidth = 194;
		imageHeight = height;
	}

	// ---- live crafting queue, to the right of the terminal ----
	private List<com.robvanblerk.tieredpower.network.JobsListPacket.Job> queue = List.of();
	private int queueTicks;
	private boolean showQueue = true;
	private static final int QUEUE_W = 118, QUEUE_ROWS = 8, QUEUE_ROW_H = 20;

	/** Where the crafting queue panel is (for JEI to keep clear of), or null when it isn't showing. */
	public net.minecraft.client.renderer.Rect2i queuePanel() {
		if (!showQueue || queue.isEmpty() || leftPos + imageWidth + 2 + QUEUE_W > width) return null;
		int rows = Math.min(QUEUE_ROWS, queue.size());
		return new net.minecraft.client.renderer.Rect2i(leftPos + imageWidth + 2, topPos, QUEUE_W, 16 + rows * QUEUE_ROW_H + (queue.size() > QUEUE_ROWS ? 10 : 0));
	}

	public void receiveJobs(com.robvanblerk.tieredpower.network.JobsListPacket packet) {
		queue = packet.jobs();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (showQueue && queueTicks++ % 20 == 0)
			ModNetwork.CHANNEL.sendToServer(new com.robvanblerk.tieredpower.network.JobsRequestPacket(menu.containerId, -1));
	}

	private void renderQueue(GuiGraphics g, int mouseX, int mouseY) {
		if (!showQueue || queue.isEmpty()) return;
		int x = leftPos + imageWidth + 2, y = topPos;
		if (x + QUEUE_W > width) return; // no room beside the terminal
		int rows = Math.min(QUEUE_ROWS, queue.size());
		GuiDraw.panel(g, x, y, QUEUE_W, 16 + rows * QUEUE_ROW_H + (queue.size() > QUEUE_ROWS ? 10 : 0));
		g.drawString(font, "Crafting (" + queue.size() + ")", x + 6, y + 5, 0x404040, false);
		for (int i = 0; i < rows; i++) {
			var job = queue.get(i);
			int ry = y + 16 + i * QUEUE_ROW_H;
			g.renderItem(job.item(), x + 5, ry);
			String line1 = String.format("%,d", job.amount()) + "x " + job.item().getHoverName().getString();
			if (font.width(line1) > QUEUE_W - 28) line1 = font.plainSubstrByWidth(line1, QUEUE_W - 32) + "..";
			g.drawString(font, line1, x + 24, ry, 0x404040, false);
			String line2 = job.left() > 0 ? job.left() + " crafts left" : job.status();
			if (font.width(line2) > QUEUE_W - 28) line2 = font.plainSubstrByWidth(line2, QUEUE_W - 32) + "..";
			boolean waiting = job.status().toLowerCase(Locale.ROOT).contains("wait") || job.status().toLowerCase(Locale.ROOT).contains("missing");
			g.drawString(font, line2, x + 24, ry + 9, waiting ? 0xB05020 : 0x2A7A2A, false);
			if (mouseX >= x && mouseX < x + QUEUE_W && mouseY >= ry - 1 && mouseY < ry + QUEUE_ROW_H - 1) {
				List<Component> tip = new ArrayList<>();
				tip.add(job.item().getHoverName());
				tip.add(Component.literal(job.status()).withStyle(ChatFormatting.GRAY));
				for (String step : job.steps()) tip.add(Component.literal(step).withStyle(ChatFormatting.DARK_GRAY));
				tip.add(Component.literal("Jobs button: details and cancel").withStyle(ChatFormatting.DARK_AQUA));
				g.renderTooltip(font, tip, Optional.empty(), mouseX, mouseY);
			}
		}
		if (queue.size() > QUEUE_ROWS) g.drawString(font, "+" + (queue.size() - QUEUE_ROWS) + " more", x + 6, y + 16 + rows * QUEUE_ROW_H, 0x606060, false);
	}

	@Override
	protected void init() {
		super.init();
		search = new EditBox(font, leftPos + 8, topPos + 5, 118, 11, Component.literal("Search"));
		search.setMaxLength(50);
		search.setBordered(true);
		search.setHint(Component.literal("Search, or @mod").withStyle(ChatFormatting.DARK_GRAY));
		addRenderableWidget(search);
		addRenderableWidget(Button.builder(Component.literal(VIEWS[view]), b -> {
			view = (view + 1) % VIEWS.length;
			b.setMessage(Component.literal(VIEWS[view]));
			scrollRow = 0;
		}).bounds(leftPos + 129, topPos + 4, 32, 13).tooltip(Tooltip.create(Component.literal("Show items, fluids or both"))).build());
		addRenderableWidget(Button.builder(Component.literal("Jobs"), b -> minecraft.setScreen(new JobsScreen(this, menu.containerId)))
				.bounds(leftPos + 156, topPos + GRID_Y + ROWS * 18 + 1, 31, 12)
				.tooltip(Tooltip.create(Component.literal("Crafting jobs: progress, and cancel"))).build());
		addRenderableWidget(Button.builder(Component.literal("123"), b -> {
			sortByName = !sortByName;
			b.setMessage(Component.literal(sortByName ? "A-Z" : "123"));
		}).bounds(leftPos + 163, topPos + 4, 24, 13).tooltip(Tooltip.create(Component.literal("Sort by amount or by name"))).build());
	}

	private static String modOf(ItemStack item, FluidStack fluid) {
		var id = item.isEmpty() ? ForgeRegistries.FLUIDS.getKey(fluid.getFluid()) : ForgeRegistries.ITEMS.getKey(item.getItem());
		return id == null ? "" : id.getNamespace();
	}

	private List<Row> visible() {
		List<StorageTerminalMenu.Entry> items = menu.getClientList();
		List<StorageTerminalMenu.FluidEntry> fluids = menu.getClientFluids();
		String q = search.getValue().trim().toLowerCase(Locale.ROOT);
		if (items == cacheItems && fluids == cacheFluids && menu.getClientCraftables() == cacheCraftables && q.equals(cacheSearch) && sortByName == cacheSort && view == cacheView) return visible;
		List<Row> out = new ArrayList<>();
		List<ItemStack> craftables = menu.getClientCraftables();
		java.util.Set<com.robvanblerk.tieredpower.storage.ItemKey> canCraft = new java.util.HashSet<>();
		for (ItemStack c : craftables) canCraft.add(new com.robvanblerk.tieredpower.storage.ItemKey(c));
		java.util.Set<com.robvanblerk.tieredpower.storage.ItemKey> stored = new java.util.HashSet<>();
		if (view != 2) {
			for (StorageTerminalMenu.Entry e : items) {
				var key = new com.robvanblerk.tieredpower.storage.ItemKey(e.item());
				stored.add(key);
				out.add(new Row(e.item(), FluidStack.EMPTY, e.count(), e.item().getHoverName().getString(), canCraft.contains(key)));
			}
			// things the network can make but has none of: shown with "Craft"
			for (ItemStack c : craftables) if (!stored.contains(new com.robvanblerk.tieredpower.storage.ItemKey(c)))
				out.add(new Row(c, FluidStack.EMPTY, 0, c.getHoverName().getString(), true));
		}
		if (view != 1) for (StorageTerminalMenu.FluidEntry e : fluids) out.add(new Row(ItemStack.EMPTY, e.fluid(), e.amount(), e.fluid().getDisplayName().getString(), false));
		if (!q.isEmpty()) {
			out.removeIf(r -> q.startsWith("@") ? !modOf(r.item(), r.fluid()).startsWith(q.substring(1)) : !r.name().toLowerCase(Locale.ROOT).contains(q));
		}
		if (sortByName) out.sort(Comparator.comparing(Row::name));
		cacheItems = items;
		cacheFluids = fluids;
		cacheCraftables = menu.getClientCraftables();
		cacheSearch = q;
		cacheSort = sortByName;
		cacheView = view;
		visible = out;
		return out;
	}

	private int maxScroll() {
		return Math.max(0, (visible().size() + COLS - 1) / COLS - ROWS);
	}

	// ---- drawing ----

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		GuiDraw.panel(g, x, y, imageWidth, imageHeight);
		for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) GuiDraw.slot(g, x + GRID_X - 1 + c * 18, y + GRID_Y - 1 + r * 18);
		for (var s : menu.slots) GuiDraw.slot(g, x + s.x - 1, y + s.y - 1);
		g.fill(x + BAR_X, y + GRID_Y - 1, x + BAR_X + 12, y + GRID_Y - 1 + ROWS * 18, 0xFF373737);
		int max = maxScroll();
		int thumbH = 15, track = ROWS * 18 - 2 - thumbH;
		int ty = y + GRID_Y + (max == 0 ? 0 : track * scrollRow / max);
		g.fill(x + BAR_X + 1, ty, x + BAR_X + 11, ty + thumbH, max == 0 ? 0xFF8B8B8B : 0xFFC6C6C6);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		StorageNetwork.Stats s = menu.getClientStats();
		String info;
		if (!s.online()) info = "Offline";
		else if (s.fluidDisks() == 0) info = String.format("%s / %s items, %d types", PowerInfo.shortFe(s.used()), PowerInfo.shortFe(s.capacity()), s.types());
		else info = String.format("%s/%s items  %s/%s B", PowerInfo.shortFe(s.used()), PowerInfo.shortFe(s.capacity()), PowerInfo.shortFe(s.fluidUsed() / 1000),
				PowerInfo.shortFe(s.fluidCapacity() / 1000));
		g.drawString(font, info, 8, GRID_Y + ROWS * 18 + 4, s.online() ? 0x404040 : 0xAA0000, false);
		g.drawString(font, playerInventoryTitle, 8, menu.invY() - 11, 0x404040, false);
	}

	/** A fluid drawn as a 16x16 square of its texture, tinted. */
	private static void drawFluid(GuiGraphics g, FluidStack fluid, int x, int y) {
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
		int tint = ext.getTintColor(fluid);
		float a = ((tint >> 24) & 0xFF) / 255f;
		RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, a <= 0 ? 1f : a);
		RenderSystem.enableBlend();
		g.blit(x, y, 0, 16, 16, sprite);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	private static String label(Row r) {
		if (r.isFluid()) return r.count() < 1000 ? r.count() + "m" : PowerInfo.shortFe(r.count() / 1000).replace(",", "") + "B";
		if (r.count() == 0 && r.craftable()) return "Craft";
		return r.count() < 1000 ? String.valueOf(r.count()) : PowerInfo.shortFe(r.count()).replace(",", "");
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderQueue(g, mouseX, mouseY);
		scrollRow = Math.min(scrollRow, maxScroll());
		List<Row> list = visible();
		Row hovered = null;
		if (!menu.getClientStats().online()) {
			g.drawCenteredString(font, "No powered Storage", leftPos + GRID_X + COLS * 9, topPos + GRID_Y + 24, 0xFF5555);
			g.drawCenteredString(font, "Controller connected", leftPos + GRID_X + COLS * 9, topPos + GRID_Y + 36, 0xFF5555);
		}
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				int i = (scrollRow + r) * COLS + c;
				if (i >= list.size()) break;
				Row e = list.get(i);
				int sx = leftPos + GRID_X + c * 18, sy = topPos + GRID_Y + r * 18;
				if (e.isFluid()) drawFluid(g, e.fluid(), sx, sy); else g.renderItem(e.item(), sx, sy);
				String n = label(e);
				g.pose().pushPose();
				g.pose().translate(0, 0, 200);
				g.pose().scale(0.5f, 0.5f, 1f);
				g.drawString(font, n, (sx + 17) * 2 - font.width(n), (sy + 12) * 2, e.isFluid() ? 0xAADDFF : (e.count() == 0 ? 0x80FF80 : 0xFFFFFF), true);
				if (e.craftable() && e.count() > 0) g.drawString(font, "+", sx * 2, sy * 2, 0x80FF80, true); // can craft more
				g.pose().popPose();
				if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
					hovered = e;
					g.fillGradient(net.minecraft.client.renderer.RenderType.guiOverlay(), sx, sy, sx + 16, sy + 16, 0x80FFFFFF, 0x80FFFFFF, 0);
				}
			}
		}
		if (hovered != null && (menu.getCarried().isEmpty() || hovered.isFluid())) {
			List<Component> tip = new ArrayList<>();
			if (hovered.isFluid()) {
				tip.add(hovered.fluid().getDisplayName());
				tip.add(Component.literal(String.format("Stored: %,d mB (%,d buckets)", hovered.count(), hovered.count() / 1000)).withStyle(ChatFormatting.AQUA));
				tip.add(Component.literal("Click with an empty bucket or tank to fill it").withStyle(ChatFormatting.GRAY));
			} else {
				tip.addAll(Screen.getTooltipFromItem(minecraft, hovered.item()));
				tip.add(Component.literal(String.format("Stored: %,d", hovered.count())).withStyle(ChatFormatting.AQUA));
				if (hovered.craftable()) tip.add(Component.literal(hovered.count() == 0 ? "Click to craft" : "Ctrl + click (or middle-click) to craft more")
						.withStyle(ChatFormatting.GREEN));
			}
			g.renderTooltip(font, tip, Optional.empty(), mouseX, mouseY);
		} else {
			renderTooltip(g, mouseX, mouseY);
		}
	}

	// ---- input ----

	private boolean inGrid(double mx, double my) {
		return mx >= leftPos + GRID_X && mx < leftPos + GRID_X + COLS * 18 && my >= topPos + GRID_Y && my < topPos + GRID_Y + ROWS * 18;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (inGrid(mx, my) && menu.getClientStats().online()) {
			int c = (int) (mx - leftPos - GRID_X) / 18, r = (int) (my - topPos - GRID_Y) / 18;
			int i = (scrollRow + r) * COLS + c;
			List<Row> list = visible();
			Row clicked = i < list.size() ? list.get(i) : null;
			boolean holding = !menu.getCarried().isEmpty();
			if (!holding && clicked != null && clicked.craftable() && (clicked.count() == 0 || hasControlDown() || button == 2)) {
				minecraft.setScreen(new CraftRequestScreen(this, menu.containerId, clicked.item()));
				return true;
			}
			if (holding && clicked != null && clicked.isFluid()) {
				ModNetwork.CHANNEL.sendToServer(new StorageActionPacket(menu.containerId, StorageTerminalMenu.ACTION_FILL_CONTAINER, ItemStack.EMPTY, clicked.fluid()));
				return true;
			}
			int action;
			if (holding) action = button == 1 ? StorageTerminalMenu.ACTION_PUT_ONE : StorageTerminalMenu.ACTION_PUT_ALL;
			else if (clicked == null || clicked.isFluid()) return true; // fluids need a container
			else if (hasShiftDown()) action = StorageTerminalMenu.ACTION_TAKE_TO_INVENTORY;
			else action = button == 1 ? StorageTerminalMenu.ACTION_TAKE_HALF : StorageTerminalMenu.ACTION_TAKE;
			ModNetwork.CHANNEL.sendToServer(new StorageActionPacket(menu.containerId, action, clicked == null ? ItemStack.EMPTY : clicked.item()));
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		scrollRow = (int) Math.max(0, Math.min(maxScroll(), scrollRow - Math.signum(delta)));
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (key == 256) { // escape
			onClose();
			return true;
		}
		if (search.isFocused()) {
			search.keyPressed(key, scan, modifiers);
			scrollRow = 0;
			return true; // don't let "E" close the screen while typing
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		if (search.isFocused()) {
			scrollRow = 0;
			return search.charTyped(c, modifiers);
		}
		return super.charTyped(c, modifiers);
	}
}
