package org.matsim.nemo.analysis;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.core.utils.collections.Tuple;
import playground.vsp.cadyts.marginals.DistanceDistribution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TripAnalysisExcelWriter {

	private final double scalingFactor;
	private List<Tuple<String, TripAnalysis>> analyses;
	private Path file;
	private DistanceDistribution expectedModalDistanceDistribution;
	private SimpleDistanceDistribution expectedDistanceDistribution;
	private Map<String, Long> expectedModalShare;

	public TripAnalysisExcelWriter(List<Tuple<String, TripAnalysis>> analyses, Path file, DistanceDistribution expectedModalDistanceDistribution, SimpleDistanceDistribution expectedDistanceDistribution, Map<String, Long> expectedModalShare, double scalingFactor) {
		this.analyses = analyses;
		this.file = file;
		this.expectedModalDistanceDistribution = expectedModalDistanceDistribution;
		this.expectedDistanceDistribution = expectedDistanceDistribution;
		this.expectedModalShare = expectedModalShare;
		this.scalingFactor = scalingFactor;
	}

	public void write() {

		XSSFWorkbook wb = new XSSFWorkbook();

		Sheet sheet = wb.createSheet("modal-distance-distribution");
		addModalDistanceDistributionHeader(sheet);
		addExpectedModalDistanceDistribution(sheet, expectedModalDistanceDistribution);
		addModalDistanceValues(sheet, expectedModalDistanceDistribution, scalingFactor);

		Sheet modalSplitSheet = wb.createSheet("modal-split");
		addModalSplitHeader(modalSplitSheet);
		addExpectedModalSplit(modalSplitSheet, expectedModalShare);
		addModalSplitValues(modalSplitSheet, scalingFactor);

		Sheet distanceDistributionSheet = wb.createSheet("distance-distribution");
		addDistanceDistributionHeader(distanceDistributionSheet);
		addExpectedDistanceDistribution(distanceDistributionSheet, expectedDistanceDistribution);
		addDistanceDistributionValues(distanceDistributionSheet, expectedDistanceDistribution, scalingFactor);

		try (OutputStream fileOut = new FileOutputStream(file.toFile())) {
			wb.write(fileOut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void addModalDistanceDistributionHeader(Sheet sheet) {
		Row titleRow = sheet.createRow(0);
		titleRow.createCell(0).setCellValue("mode");
		titleRow.createCell(1).setCellValue("lower limit");
		titleRow.createCell(2).setCellValue("upper limit");
		titleRow.createCell(3).setCellValue("expected");

		int cellIndex = 4;

		for (Tuple<String, TripAnalysis> analysis : analyses) {
			titleRow.createCell(cellIndex).setCellValue(analysis.getFirst());
			cellIndex++;
		}
	}

	private void addExpectedModalDistanceDistribution(Sheet sheet, DistanceDistribution distanceDistribution) {

		List<DistanceDistribution.DistanceBin> expectedBins = distanceDistribution.getDistanceBins().stream()
				.sorted(this::compareBinsByModeAndDistanceRange)
				.collect(Collectors.toList());

		int rowIndex = 1; // start with one, since we used 0 for header
		int expectedSum = 0;
		for (DistanceDistribution.DistanceBin expectedBin : expectedBins) {

			Row row = sheet.createRow(rowIndex);
			row.createCell(0).setCellValue(expectedBin.getMode());
			row.createCell(1).setCellValue(expectedBin.getDistanceRange().getLowerLimit());
			row.createCell(2).setCellValue(expectedBin.getDistanceRange().getUpperLimit());
			row.createCell(3).setCellValue(expectedBin.getValue());
			expectedSum += expectedBin.getValue();
			rowIndex++;
		}
		Row expectedSumRow = sheet.createRow(rowIndex);
		expectedSumRow.createCell(0).setCellValue("sum");
		expectedSumRow.createCell(3).setCellValue(expectedSum);

		Row errorRow = sheet.createRow(rowIndex + 1);
		errorRow.createCell(0).setCellValue("error");
		errorRow.createCell(3).setCellValue(0);
	}

	private void addModalDistanceValues(Sheet sheet, DistanceDistribution expectedModalDistanceDistribution, double scalingFactor) {

		int rowIndex = 1;
		int cellIndex = 4;

		for (Tuple<String, TripAnalysis> analysis : analyses) {

			List<DistanceDistribution.DistanceBin> bins = analysis.getSecond().calculateModalDistanceDistribution(expectedModalDistanceDistribution).getDistanceBins().stream()
					.sorted(this::compareBinsByModeAndDistanceRange)
					.collect(Collectors.toList());

			int distributionSum = 0;
			int errorSum = 0;
			for (DistanceDistribution.DistanceBin bin : bins) {

				double value = bin.getValue() * scalingFactor;
				sheet.getRow(rowIndex).createCell(cellIndex).setCellValue(value);
				distributionSum += value;

				DistanceDistribution.DistanceBin xBin = expectedModalDistanceDistribution.getBin(bin.getId());
				errorSum += (value - xBin.getValue()) * (value - xBin.getValue());
				rowIndex++;
			}
			sheet.getRow(rowIndex).createCell(cellIndex).setCellValue(distributionSum);
			sheet.getRow(rowIndex + 1).createCell(cellIndex).setCellValue(errorSum);

			rowIndex = 1;
			cellIndex++;
		}
	}

	private void addModalSplitHeader(Sheet sheet) {
		Row modalTitleRow = sheet.createRow(0);
		modalTitleRow.createCell(0).setCellValue("mode");
		modalTitleRow.createCell(1).setCellValue("expected count");

		int cellIndex = 2;

		for (Tuple<String, TripAnalysis> analysis : analyses) {
			modalTitleRow.createCell(cellIndex).setCellValue(analysis.getFirst());
			cellIndex++;
		}
	}

	private void addExpectedModalSplit(Sheet sheet, Map<String, Long> expectedModalShare) {

		List<Map.Entry<String, Long>> sorted = expectedModalShare.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toList());

		int rowIndex = 1;
		int expectedSum = 0;

		for (Map.Entry<String, Long> share : sorted) {
			Row row = sheet.createRow(rowIndex);
			row.createCell(0).setCellValue(share.getKey());
			row.createCell(1).setCellValue(share.getValue());
			expectedSum += share.getValue();
			rowIndex++;
		}
		sheet.createRow(rowIndex).createCell(0).setCellValue("sum");
		sheet.getRow(rowIndex).createCell(1).setCellValue(expectedSum);
		sheet.createRow(rowIndex + 1).createCell(0).setCellValue("error-squared");
		sheet.getRow(rowIndex + 1).createCell(1).setCellValue(0);
	}

	private void addModalSplitValues(Sheet sheet, double scalingFactor) {

		int rowIndex = 1;
		int celIndex = 2;


		for (Tuple<String, TripAnalysis> analysis : analyses) {
			int sum = 0;
			double errorSum = 0;

			List<Map.Entry<String, Long>> modalSplit = analysis.getSecond().calculateModalSplit().entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.collect(Collectors.toList());

			for (Map.Entry<String, Long> split : modalSplit) {
				double value = split.getValue() * scalingFactor;
				sheet.getRow(rowIndex).createCell(celIndex).setCellValue(value);
				sum += value;
				errorSum = (expectedModalShare.get(split.getKey()) - value) * (expectedModalShare.get(split.getKey()) - value);
				rowIndex++;
			}
			sheet.getRow(rowIndex).createCell(celIndex).setCellValue(sum);
			sheet.getRow(rowIndex + 1).createCell(celIndex).setCellValue(errorSum);
			celIndex++;
			rowIndex = 1;
		}
	}

	private void addDistanceDistributionHeader(Sheet sheet) {

		Row titleRow = sheet.createRow(0);
		titleRow.createCell(0).setCellValue("lower-limit");
		titleRow.createCell(1).setCellValue("upper-limit");
		titleRow.createCell(2).setCellValue("expected");

		int cellIndex = 3;

		for (Tuple<String, TripAnalysis> analysis : analyses) {
			titleRow.createCell(cellIndex).setCellValue(analysis.getFirst());
			cellIndex++;
		}
	}

	private void addExpectedDistanceDistribution(Sheet sheet, SimpleDistanceDistribution expectedDistanceDistribution) {

		List<SimpleDistanceDistribution.SimpleDistanceBin> sorted = expectedDistanceDistribution.getDistanceBins().stream()
				.sorted(Comparator.comparingDouble(b -> b.getDistanceRange().getLowerLimit()))
				.collect(Collectors.toList());

		int rowIndex = 1;
		int sum = 0;

		for (SimpleDistanceDistribution.SimpleDistanceBin bin : sorted) {
			Row row = sheet.createRow(rowIndex);
			row.createCell(0).setCellValue(bin.getDistanceRange().getLowerLimit());
			row.createCell(1).setCellValue(bin.getDistanceRange().getUpperLimit());
			row.createCell(2).setCellValue(bin.getValue());
			sum += bin.getValue();
			rowIndex++;
		}

		sheet.createRow(rowIndex).createCell(2).setCellValue(sum);
		sheet.createRow(rowIndex + 1).createCell(2).setCellValue(0);
	}

	private void addDistanceDistributionValues(Sheet sheet, SimpleDistanceDistribution expectedDistanceDistribution, double scalingFactor) {

		int rowIndex = 1;
		int cellIndex = 3;


		for (Tuple<String, TripAnalysis> analysis : analyses) {

			int sum = 0;
			double errorSum = 0;
			List<SimpleDistanceDistribution.SimpleDistanceBin> sorted = analysis.getSecond().calculateDistanceDistribution(expectedDistanceDistribution).getDistanceBins().stream()
					.sorted(Comparator.comparingDouble(b -> b.getDistanceRange().getLowerLimit()))
					.collect(Collectors.toList());

			for (SimpleDistanceDistribution.SimpleDistanceBin bin : sorted) {

				SimpleDistanceDistribution.SimpleDistanceBin expectedBin = expectedDistanceDistribution.getDistanceBins().stream()
						.filter(xbin -> xbin.getDistanceRange().getLowerLimit() == bin.getDistanceRange().getLowerLimit())
						.findFirst().orElseThrow(() -> new RuntimeException("couldn't find bin"));
				double value = bin.getValue() * scalingFactor;
				sheet.getRow(rowIndex).createCell(cellIndex).setCellValue(value);
				sum += value;
				errorSum = (value - expectedBin.getValue()) * (value - expectedBin.getValue());
				rowIndex++;
			}

			sheet.getRow(rowIndex).createCell(cellIndex).setCellValue(sum);
			sheet.getRow(rowIndex + 1).createCell(cellIndex).setCellValue(errorSum);
			cellIndex++;
			rowIndex = 1;
		}
	}

	private int compareBinsByModeAndDistanceRange(DistanceDistribution.DistanceBin bin1, DistanceDistribution.DistanceBin bin2) {
		int mode = bin1.getMode().compareTo(bin2.getMode());
		return (mode == 0) ? Double.compare(bin1.getDistanceRange().getLowerLimit(), bin2.getDistanceRange().getLowerLimit()) : mode;
	}

	public static class Builder {

		private DistanceDistribution expectedModalDistanceDistribution;
		private SimpleDistanceDistribution expectedDistanceDistribution;
		private Map<String, Long> expectedModalShare;
		private List<Tuple<String, TripAnalysis>> analyses;
		private Path file;
		private double scalingFactor = 1.0;

		public Builder addExpectedModalDistanceDistribution(DistanceDistribution expected) {
			this.expectedModalDistanceDistribution = expected;
			return this;
		}

		public Builder addExpectedDistanceDistribution(SimpleDistanceDistribution expected) {
			this.expectedDistanceDistribution = expected;
			return this;
		}

		public Builder addExpectedModalShare(Map<String, Long> expected) {
			this.expectedModalShare = expected;
			return this;
		}

		public Builder tripAnalysises(List<Tuple<String, TripAnalysis>> analyses) {
			this.analyses = analyses;
			return this;
		}

		public Builder filePath(Path file) {
			this.file = file;
			return this;
		}

		public Builder scalingFactor(double scalingFactor) {
			this.scalingFactor = scalingFactor;
			return this;
		}

		public TripAnalysisExcelWriter build() {

			if (file == null || analyses == null) {
				throw new IllegalArgumentException("file path and analysises are required");
			}

			return new TripAnalysisExcelWriter(analyses, file, expectedModalDistanceDistribution, expectedDistanceDistribution, expectedModalShare, scalingFactor);
		}
	}
}
