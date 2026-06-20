"use client";

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useVendorProfile, useUpdateVendorProfile } from "@/features/vendor/hooks/useVendorPortalQueries";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { StatusChip } from "@/components/ui/status-chip";
import { useToast } from "@/components/ui/toast";

const profileSchema = z.object({
  businessName: z.string().min(2, "Business name is required"),
  ownerName: z.string().min(2, "Owner name is required"),
  phone: z
    .string()
    .min(10, "Phone number must be at least 10 characters")
    .max(15, "Phone number must not exceed 15 characters")
    .regex(/^\+?[0-9]{10,15}$/, "Invalid phone number format"),
  foodType: z.string().min(2, "Food type is required"),
  description: z.string().min(10, "Please provide a more detailed description"),
  address: z.string().min(5, "Address is required"),
  latitude: z.coerce
    .number()
    .min(-90, "Latitude must be between -90 and 90")
    .max(90, "Latitude must be between -90 and 90"),
  longitude: z.coerce
    .number()
    .min(-180, "Longitude must be between -180 and 180")
    .max(180, "Longitude must be between -180 and 180"),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export default function VendorProfilePage() {
  const { data: profile, isLoading, isError, error, refetch } = useVendorProfile();
  const updateMutation = useUpdateVendorProfile();
  const [isEditing, setIsEditing] = useState(false);
  const { addToast } = useToast();

  const getProfileStatusVariant = (status: string): "SUCCESS" | "WARNING" | "DANGER" | "INFO" => {
    switch (status) {
      case 'APPROVED': return 'SUCCESS';
      case 'REJECTED': return 'DANGER';
      case 'PENDING_REVIEW': return 'WARNING';
      default: return 'INFO';
    }
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(profileSchema) as any,
  });

  // Populate form when data loads or editing is toggled
  useEffect(() => {
    if (profile && isEditing) {
      reset({
        businessName: profile.businessName || "",
        ownerName: profile.ownerName || "",
        phone: profile.phone || "",
        foodType: profile.foodType || "",
        description: profile.description || "",
        address: profile.address || "",
        latitude: profile.latitude || 0,
        longitude: profile.longitude || 0,
      });
    }
  }, [profile, isEditing, reset]);

  if (isLoading) {
    return <LoadingState message="Loading your profile..." />;
  }

  if (isError || !profile) {
    return (
      <ErrorState
        title="Failed to load profile"
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const onSubmit = (data: ProfileFormValues) => {
    updateMutation.mutate(data, {
      onSuccess: () => {
        addToast({
          title: "Profile Updated",
          description: "Your vendor profile has been updated successfully.",
          type: "success",
        });
        setIsEditing(false);
      },
      onError: (err) => {
        addToast({
          title: "Update Failed",
          description: err instanceof Error ? err.message : "Failed to update profile",
          type: "error",
        });
      },
    });
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-text-primary">Store Profile</h1>
          <p className="text-text-secondary mt-1">Manage your public storefront details.</p>
        </div>
        <div className="flex items-center gap-3">
          <StatusChip status={getProfileStatusVariant(profile.status)} label={profile.status.replace('_', ' ')} />
          {!isEditing && (
            <Button onClick={() => setIsEditing(true)}>Edit Profile</Button>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Business Information</CardTitle>
          <CardDescription>These details are visible to customers on the map and discovery pages.</CardDescription>
        </CardHeader>
        <CardContent>
          {isEditing ? (
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Business Name</label>
                  <Input {...register("businessName")} placeholder="Bob's Burgers" />
                  {errors.businessName && <p className="text-sm text-red-500">{errors.businessName.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Owner Name</label>
                  <Input {...register("ownerName")} placeholder="Bob Belcher" />
                  {errors.ownerName && <p className="text-sm text-red-500">{errors.ownerName.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Phone Number</label>
                  <Input {...register("phone")} placeholder="+1234567890" />
                  {errors.phone && <p className="text-sm text-red-500">{errors.phone.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Food Type</label>
                  <Input {...register("foodType")} placeholder="Burgers, American" />
                  {errors.foodType && <p className="text-sm text-red-500">{errors.foodType.message}</p>}
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium">Description</label>
                <Textarea {...register("description")} placeholder="Describe your food and story..." className="min-h-[100px]" />
                {errors.description && <p className="text-sm text-red-500">{errors.description.message}</p>}
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium">Address</label>
                <Input {...register("address")} placeholder="123 Ocean Ave, Wagstaff City" />
                {errors.address && <p className="text-sm text-red-500">{errors.address.message}</p>}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Latitude</label>
                  <Input type="number" step="any" {...register("latitude")} />
                  {errors.latitude && <p className="text-sm text-red-500">{errors.latitude.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Longitude</label>
                  <Input type="number" step="any" {...register("longitude")} />
                  {errors.longitude && <p className="text-sm text-red-500">{errors.longitude.message}</p>}
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4">
                <Button type="button" variant="outline" onClick={() => setIsEditing(false)} disabled={updateMutation.isPending}>
                  Cancel
                </Button>
                <Button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? "Saving..." : "Save Changes"}
                </Button>
              </div>
            </form>
          ) : (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <p className="text-sm font-medium text-text-secondary">Business Name</p>
                  <p className="mt-1">{profile.businessName}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-text-secondary">Owner Name</p>
                  <p className="mt-1">{profile.ownerName}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-text-secondary">Phone Number</p>
                  <p className="mt-1">{profile.phone}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-text-secondary">Food Type</p>
                  <p className="mt-1">{profile.foodType}</p>
                </div>
              </div>
              
              <div>
                <p className="text-sm font-medium text-text-secondary">Description</p>
                <p className="mt-1 whitespace-pre-wrap">{profile.description}</p>
              </div>

              <div>
                <p className="text-sm font-medium text-text-secondary">Address</p>
                <p className="mt-1">{profile.address}</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <p className="text-sm font-medium text-text-secondary">Coordinates</p>
                  <p className="mt-1 text-sm font-mono bg-slate-100 px-2 py-1 rounded inline-block">
                    {profile.latitude}, {profile.longitude}
                  </p>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
