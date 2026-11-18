import { Asset } from "expo-asset";
import { File } from "expo-file-system";
import { fetchImageAsBase64 } from "./printer-utils";

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

export const loadReceiptBase64 = async (
  receipt: ReceiptOption,
  imageUrl: string,
): Promise<string> => {
  if (receipt === "URL") return fetchImageAsBase64(imageUrl);
  const asset = await Asset.fromModule(RECEIPT_ASSETS[receipt]).downloadAsync();
  if (!asset.localUri) throw new Error(`Cannot load the ${receipt} image`);
  return new File(asset.localUri).base64();
};
