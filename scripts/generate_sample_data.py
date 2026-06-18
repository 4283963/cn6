#!/usr/bin/env python3
"""
飞机传感器数据 CSV 示例生成脚本
用于生成测试用的飞行日志数据
"""

import csv
import random
import math
from datetime import datetime, timedelta
import argparse
import sys


def generate_flight_data(flight_number, hours=2, interval_seconds=1, output_file=None):
    """
    生成模拟飞行数据

    Args:
        flight_number: 航班号，如 CA1234
        hours: 飞行时长（小时）
        interval_seconds: 采样间隔（秒）
        output_file: 输出文件路径
    """
    if output_file is None:
        output_file = f"{flight_number}_flight_data.csv"

    total_seconds = hours * 3600
    total_points = total_seconds // interval_seconds

    start_time = datetime.now() - timedelta(hours=hours)

    print(f"航班号: {flight_number}")
    print(f"飞行时长: {hours} 小时")
    print(f"采样间隔: {interval_seconds} 秒")
    print(f"总数据点: {total_points:,}")
    print(f"输出文件: {output_file}")

    headers = [
        "timestamp",
        "altitude",
        "speed",
        "engineTemperature",
        "fuelConsumption",
        "cabinPressure",
        "latitude",
        "longitude",
        "verticalSpeed",
        "heading"
    ]

    climb_duration = int(total_points * 0.15)
    cruise_duration = int(total_points * 0.70)
    descent_duration = total_points - climb_duration - cruise_duration

    max_altitude = random.uniform(9000, 12500)
    cruise_speed = random.uniform(800, 950)
    engine_temp_cruise = random.uniform(620, 680)

    start_lat = random.uniform(30, 45)
    start_lon = random.uniform(110, 125)
    end_lat = random.uniform(20, 40)
    end_lon = random.uniform(110, 135)

    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(headers)

        for i in range(total_points):
            current_time = start_time + timedelta(seconds=i * interval_seconds)
            phase = i / total_points

            if i < climb_duration:
                p = i / climb_duration
                altitude = max_altitude * (1 - math.cos(p * math.pi / 2))
                speed = min(cruise_speed, 200 + p * (cruise_speed - 200))
                engine_temp = min(engine_temp_cruise, 500 + p * (engine_temp_cruise - 500))
                vertical_speed = 12 + math.sin(p * math.pi) * 6

            elif i < climb_duration + cruise_duration:
                p = (i - climb_duration) / cruise_duration if cruise_duration > 0 else 0
                turbulence = random.uniform(-80, 80)
                altitude = max_altitude + turbulence + math.sin(p * 20) * 30
                speed = cruise_speed + random.uniform(-30, 30) + math.sin(p * 10) * 15
                engine_temp = engine_temp_cruise + random.uniform(-20, 20) + math.sin(p * 8) * 10
                vertical_speed = random.uniform(-2, 2) + math.sin(p * 50) * 1.5

            else:
                p = (i - climb_duration - cruise_duration) / descent_duration if descent_duration > 0 else 0
                altitude = max_altitude * math.cos(p * math.pi / 2)
                speed = cruise_speed - p * (cruise_speed - 220)
                engine_temp = engine_temp_cruise - p * (engine_temp_cruise - 480)
                vertical_speed = -10 - math.sin(p * math.pi) * 8

            altitude = max(0, altitude)
            speed = max(0, speed)
            engine_temp = max(0, engine_temp)

            fuel_rate = 2.5 + (speed / cruise_speed) * 4.5 + (engine_temp / 800) * 1.5
            fuel_consumption = fuel_rate * interval_seconds + random.uniform(-0.3, 0.3)
            fuel_consumption = max(0, fuel_consumption)

            cabin_altitude_pressure = 80000 + (altitude / max_altitude) * (-10000) if altitude > 0 else 101325
            cabin_pressure = cabin_altitude_pressure / 1000 + random.uniform(-0.5, 0.5)
            cabin_pressure = max(0, cabin_pressure)

            lat = start_lat + (end_lat - start_lat) * phase + random.uniform(-0.01, 0.01)
            lon = start_lon + (end_lon - start_lon) * phase + random.uniform(-0.01, 0.01)

            base_heading = math.degrees(math.atan2(end_lat - start_lat, end_lon - start_lon))
            if base_heading < 0:
                base_heading += 360
            heading = (base_heading + random.uniform(-5, 5) + math.sin(phase * 50) * 3) % 360

            writer.writerow([
                current_time.strftime("%Y-%m-%d %H:%M:%S"),
                f"{altitude:.2f}",
                f"{speed:.2f}",
                f"{engine_temp:.2f}",
                f"{fuel_consumption:.4f}",
                f"{cabin_pressure:.2f}",
                f"{lat:.6f}",
                f"{lon:.6f}",
                f"{vertical_speed:.2f}",
                f"{heading:.1f}"
            ])

            if (i + 1) % 50000 == 0:
                progress = (i + 1) / total_points * 100
                print(f"  进度: {progress:.1f}% ({i + 1:,}/{total_points:,} 条)")

    import os
    file_size = os.path.getsize(output_file)
    size_mb = file_size / (1024 * 1024)
    print(f"\n✓ 生成完成! 文件大小: {size_mb:.2f} MB")
    print(f"  预计上传解析耗时: 取决于服务器性能，约 {max(10, int(total_points / 20000))} 秒")


def main():
    parser = argparse.ArgumentParser(description='生成飞机飞行传感器模拟数据 CSV')
    parser.add_argument('-f', '--flight', default='CA1234', help='航班号 (默认: CA1234)')
    parser.add_argument('-H', '--hours', type=float, default=2, help='飞行时长小时数 (默认: 2)')
    parser.add_argument('-i', '--interval', type=int, default=1, help='采样间隔秒数 (默认: 1)')
    parser.add_argument('-o', '--output', help='输出文件路径')

    args = parser.parse_args()

    try:
        generate_flight_data(
            flight_number=args.flight,
            hours=args.hours,
            interval_seconds=args.interval,
            output_file=args.output
        )
    except KeyboardInterrupt:
        print("\n操作已取消")
        sys.exit(1)


if __name__ == '__main__':
    main()
