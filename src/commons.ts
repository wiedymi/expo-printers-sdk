export type PrinterConnectionType = "Bluetooth" | "Network" | "USB";

// Paper loaded in the printer, in millimeters. Most printers cannot report it,
// so set it when it is not the printer's standard paper.
export type PaperWidth = 58 | 80;

export type PrintOptions = {
  paperWidth?: PaperWidth;
};
