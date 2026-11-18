import { Asset } from "expo-asset";
import { File } from "expo-file-system";

export type ReceiptOption = "SHORT" | "LONG" | "CHART" | "URL";

export const RECEIPT_LABELS: Record<ReceiptOption, string> = {
  SHORT: "Short",
  LONG: "Long",
  CHART: "Test chart",
  URL: "URL",
};

// Sample images: a short and a long receipt 560 px wide, and a test chart 576 px wide with edge marks, line
// widths, gray steps and text sizes.
const RECEIPT_ASSETS: Record<Exclude<ReceiptOption, "URL">, number> = {
  SHORT: require("../assets/receipts/receipt-short.png"),
  LONG: require("../assets/receipts/receipt-long.png"),
  CHART: require("../assets/receipts/test-chart.png"),
};

const fetchImageAsBase64 = async (imageUrl: string): Promise<string> => {
  const response = await fetch(imageUrl);
  if (!response.ok) {
    throw new Error(`Cannot load the image: HTTP ${response.status}`);
  }
  const blob = await response.blob();
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(",")[1] ?? "");
    reader.onerror = () => reject(new Error("Cannot read the image"));
    reader.readAsDataURL(blob);
  });
};

// The bundled images do not change, so each one is read only once.
const assetBase64 = new Map<number, Promise<string>>();

const loadAssetAsBase64 = async (assetModule: number): Promise<string> => {
  const asset = Asset.fromModule(assetModule);
  // In an Android release build, expo-asset gives a bundled image its drawable resource name as localUri, not
  // a file. Clear it, so that downloadAsync copies the image to a file in the cache.
  if (asset.localUri && !asset.localUri.includes(":")) {
    asset.localUri = null;
    asset.downloaded = false;
  }
  await asset.downloadAsync();
  if (!asset.localUri) throw new Error(`Cannot load ${asset.name}`);
  return new File(asset.localUri).base64();
};

const cachedAssetBase64 = (assetModule: number): Promise<string> => {
  let base64 = assetBase64.get(assetModule);
  if (!base64) {
    base64 = loadAssetAsBase64(assetModule);
    // A failed read is tried again on the next call.
    base64.catch(() => assetBase64.delete(assetModule));
    assetBase64.set(assetModule, base64);
  }
  return base64;
};

export const loadReceiptBase64 = (
  receipt: ReceiptOption,
  imageUrl: string,
): Promise<string> =>
  receipt === "URL"
    ? fetchImageAsBase64(imageUrl)
    : cachedAssetBase64(RECEIPT_ASSETS[receipt]);
